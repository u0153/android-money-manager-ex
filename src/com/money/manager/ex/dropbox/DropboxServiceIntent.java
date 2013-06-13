package com.money.manager.ex.dropbox;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.money.manager.ex.BuildConfig;
import com.money.manager.ex.MainActivity;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.dropbox.DropboxHelper.OnDownloadUploadEntry;

public class DropboxServiceIntent extends IntentService {
	private static final String LOGCAT = DropboxServiceIntent.class.getSimpleName();
	// intent action
	public static final String INTENT_ACTION_SYNC = "com.money.manager.ex.custom.intent.action.DROPBOX_SYNC";
	public static final String INTENT_ACTION_DOWNLOAD = "com.money.manager.ex.custom.intent.action.DROPBOX_DOWNLOAD";
	public static final String INTENT_ACTION_UPLOAD = "com.money.manager.ex.custom.intent.action.DROPBOX_UPLOAD";
	// intent extra
	public static final String INTENT_EXTRA_LOCAL_FILE = "DropboxServiceIntent:LocalFile";
	public static final String INTENT_EXTRA_REMOTE_FILE = "DropboxServiceIntent:RemoteFile";
	// id notification
	public static final int NOTIFICATION_DROPBOX_PROGRESS = 0xCCCC;
	public static final int NOTIFICATION_DROPBOX_OPEN_FILE = 0xDDDD;
	// instance dropbox
	private DropboxHelper mDropboxHelper; 
	
	public DropboxServiceIntent() {
		super("com.money.manager.ex.dropbox.DropboxServiceIntent");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (BuildConfig.DEBUG) Log.d(LOGCAT, intent.toString());
		// check if device is online
		Core core = new Core(getApplicationContext());
		if (!core.isOnline()) {
			if (BuildConfig.DEBUG) Log.d(LOGCAT, "The device is not connected to internet");
			return;
		}
		// take instance dropbox
		mDropboxHelper = DropboxHelper.getInstance(getApplicationContext());
		// check if connect to dropbox
		mDropboxHelper.isLinked();
		// take local and remote files
		String local = intent.getStringExtra(INTENT_EXTRA_LOCAL_FILE);
		String remote = intent.getStringExtra(INTENT_EXTRA_REMOTE_FILE);
		// check if file is correct
		if (TextUtils.isEmpty(local) || TextUtils.isEmpty(remote)) return;
		// take a file and entries
		File localFile = new File(local);
		Entry remoteFile = mDropboxHelper.getEntry(remote);
		// check if local file or remote file is null, then exit
		if (remoteFile == null && INTENT_ACTION_UPLOAD.equals(intent.getAction())) {
			Log.w(LOGCAT, "remoteFile is null. DropboxServiceIntent.onHandleIntent force create remote file auto");
			remoteFile = new Entry();
			remoteFile.path = remote;
		} else if (remoteFile == null) {
			Log.e(LOGCAT, "remoteFile is null. DropboxServiceIntent.onHandleIntent don't execute");
			return;
		}
		// check if name is same
		if (!localFile.getName().toUpperCase().equals(remoteFile.fileName().toUpperCase())) return;
		// check action
		if (INTENT_ACTION_DOWNLOAD.equals(intent.getAction())) {
			downloadFile(localFile, remoteFile);
		} else if (INTENT_ACTION_UPLOAD.equals(intent.getAction())) {
			uploadFile(localFile, remoteFile);
		} else {
			// Synchronization
			syncFile(localFile, remoteFile);
		}
	}
	
	public void syncFile(final File localFile, final Entry remoteFile) {
		Date localLastModified = null, remoteLastModified = null;
		try {
			localLastModified = mDropboxHelper.getDateLastModified(remoteFile.fileName());
		} catch (Exception e) {
			Log.e(LOGCAT, e.getMessage());
		}
		if (localLastModified == null)
			localLastModified = new Date(localFile.lastModified());
		remoteLastModified = mDropboxHelper.getLastModifiedEntry(remoteFile);
		if (BuildConfig.DEBUG) Log.d(LOGCAT, "Date last modified local file: " + new SimpleDateFormat().format(localLastModified));
		if (BuildConfig.DEBUG) Log.d(LOGCAT, "Date last modified remote file: " + new SimpleDateFormat().format(remoteLastModified));
		// check date
		if (remoteLastModified.after(localLastModified)) {
			if (BuildConfig.DEBUG) Log.d(LOGCAT, "Download " + remoteFile.path + " from Dropox");
			// download file
			downloadFile(localFile, remoteFile);
		} else if (remoteLastModified.before(localLastModified)) {
			if (BuildConfig.DEBUG) Log.d(LOGCAT, "Upload " + localFile.getPath() + " to Dropox");
			// upload file
			uploadFile(localFile, remoteFile);
		} else {
			if (BuildConfig.DEBUG) Log.d(LOGCAT, "The local and remote files are the same");
		}
	}

	public void downloadFile(final File localFile, final Entry remoteFile) {
		final NotificationCompat.Builder notification = mDropboxHelper.getNotificationBuilderDownload();
		// get istance notification manager
		final NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		// create interface
		OnDownloadUploadEntry onDownloadUpload = new OnDownloadUploadEntry() {
			@Override
			public void onPreExecute() {
				if (notification != null && notificationManager != null) {
					notificationManager.notify(NOTIFICATION_DROPBOX_PROGRESS, notification.build());
				}
			}

			@Override
			public void onPostExecute(boolean result) {
				if (notification != null && notificationManager != null) {
					notificationManager.cancel(NOTIFICATION_DROPBOX_PROGRESS);
					if (result) {
						// create notification for open file
						// pending intent
						Intent intent = new Intent(getApplicationContext(), MainActivity.class);
						intent.setData(Uri.fromFile(localFile));
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), MainActivity.REQUEST_PICKFILE_CODE, intent, 0);
						// create builder
						final NotificationCompat.Builder notification = mDropboxHelper.getNotificationBuilderDownloadComplete(pendingIntent);
						// notify
						notificationManager.notify(NOTIFICATION_DROPBOX_OPEN_FILE, notification.build());
					}
				}
			}
		};
		// create listener
		ProgressListener listener = new ProgressListener() {
			@Override
			public void onProgress(long bytes, long total) {
				notificationManager.notify(NOTIFICATION_DROPBOX_PROGRESS, mDropboxHelper.getNotificationBuilderProgress(notification, (int)total, (int)bytes).build());
			}
		};
		if (BuildConfig.DEBUG) Log.d(LOGCAT, "Download file from Dropbox. Local file: " + localFile.getPath() + "; Remote file: " + remoteFile.path);
		//mDropboxHelper.downloadFile(remoteFile, localFile, onDownloadUpload, listener);
		onDownloadUpload.onPreExecute();
		boolean ret = mDropboxHelper.download(remoteFile, localFile, listener);
		onDownloadUpload.onPostExecute(ret);
	}
	
	public void uploadFile(final File localFile, final Entry remoteFile) {
		final NotificationCompat.Builder notification = mDropboxHelper.getNotificationBuilderUpload();
		// get instance notification manager
		final NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		// create interface
		OnDownloadUploadEntry onDownloadUpload = new OnDownloadUploadEntry() {
			@Override
			public void onPreExecute() {
				if (notification != null && notificationManager != null) {
					notificationManager.notify(NOTIFICATION_DROPBOX_PROGRESS, notification.build());
				}
			}

			@Override
			public void onPostExecute(boolean result) {
				if (notification != null && notificationManager != null) {
					notificationManager.cancel(NOTIFICATION_DROPBOX_PROGRESS);
					if (result) {
						// create notification for open file
						// pending intent
						Intent intent = new Intent(getApplicationContext(), MainActivity.class);
						intent.setData(Uri.fromFile(localFile));
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), MainActivity.REQUEST_PICKFILE_CODE, intent, 0);
						// notification
						final NotificationCompat.Builder notification = mDropboxHelper.getNotificationBuilderUploadComplete(pendingIntent);
						// notify
						notificationManager.notify(NOTIFICATION_DROPBOX_OPEN_FILE, notification.build());
					}
				}
			}
		};
		// create listener
		ProgressListener listener = new ProgressListener() {
			@Override
			public void onProgress(long bytes, long total) {
				notificationManager.notify(NOTIFICATION_DROPBOX_PROGRESS, mDropboxHelper.getNotificationBuilderProgress(notification, (int)total, (int)bytes).build());
			}
		};
		if (BuildConfig.DEBUG) Log.d(LOGCAT, "Upload file from Dropbox. Local file: " + localFile.getPath() + "; Remote file: " + remoteFile.path);
		//mDropboxHelper.uploadFile(remoteFile.path, localFile, onDownloadUpload, listener);
		onDownloadUpload.onPreExecute();
		boolean ret = mDropboxHelper.upload(remoteFile.path, localFile, listener);
		onDownloadUpload.onPostExecute(ret);
	}
	
}