package com.money.manager.ex.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.money.manager.ex.Constants;
import com.money.manager.ex.MainActivity;
import com.money.manager.ex.MoneyManagerApplication;
import com.money.manager.ex.R;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.CurrencyUtils;
import com.money.manager.ex.database.TableCurrencyFormats;
import com.money.manager.ex.fragment.BaseFragmentActivity;
import com.money.manager.ex.preferences.PreferencesConstant;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

public class GeneralSettingsActivity extends BaseFragmentActivity {
    private static String LOGCAT = GeneralSettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralSettingsFragment()).commit();
    }

    public static class GeneralSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.general_settings);
            PreferenceManager.getDefaultSharedPreferences(getActivity());

            final MoneyManagerApplication application = (MoneyManagerApplication) getActivity().getApplication();
            final Core core = new Core(getActivity());
            final CurrencyUtils currencyUtils = new CurrencyUtils(getActivity());

            // application locale
            final ListPreference lstLocaleApp = (ListPreference) findPreference(PreferencesConstant.PREF_LOCALE);
            if (lstLocaleApp != null) {
                String summary = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString(PreferencesConstant.PREF_LOCALE, "");
                setSummaryListPreference(lstLocaleApp, summary, R.array.application_locale_values, R.array.application_locale_entries);
                lstLocaleApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        setSummaryListPreference(preference, String.valueOf(newValue), R.array.application_locale_values, R.array.application_locale_entries);
                        MainActivity.setRestartActivity(true);
                        return true;
                    }
                });
            }
            // preference username
            final Preference pUserName = findPreference(PreferencesConstant.PREF_USER_NAME);
            if (pUserName != null) {
                pUserName.setSummary(application.getUserName());
                pUserName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        application.setUserName((String) newValue, true);
                        pUserName.setSummary(application.getUserName());
                        return true;
                    }
                });
            }

            // list date format
            final ListPreference lstDateFormat = (ListPreference) findPreference(PreferencesConstant.PREF_DATE_FORMAT);
            if (lstDateFormat != null) {
                lstDateFormat.setEntries(getResources().getStringArray(R.array.date_format));
                lstDateFormat.setEntryValues(getResources().getStringArray(R.array.date_format_mask));
                //set summary
                String value = core.getInfoValue(Constants.INFOTABLE_DATEFORMAT);
                lstDateFormat.setSummary(getDateFormatFromMask(value));
                lstDateFormat.setValue(value);
                //on change
                lstDateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (core.setInfoValue(Constants.INFOTABLE_DATEFORMAT, (String) newValue)) {
                            lstDateFormat.setSummary(getDateFormatFromMask((String) newValue));
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            }

            // list preference base currency
            final ListPreference lstBaseCurrency = (ListPreference) findPreference(PreferencesConstant.PREF_BASE_CURRENCY);
            if (lstBaseCurrency != null) {
                List<TableCurrencyFormats> currencies = currencyUtils.getAllCurrencyFormats();
                String[] entries = new String[currencies.size()];
                String[] entryValues = new String[currencies.size()];
                // list of currency
                for (int i = 0; i < currencies.size(); i++) {
                    entries[i] = currencies.get(i).getCurrencyName();
                    entryValues[i] = ((Integer) currencies.get(i).getCurrencyId()).toString();
                }
                // set value
                lstBaseCurrency.setEntries(entries);
                lstBaseCurrency.setEntryValues(entryValues);
                TableCurrencyFormats tableCurrency = currencyUtils.getTableCurrencyFormats(currencyUtils.getBaseCurrencyId());
                if (tableCurrency != null) {
                    lstBaseCurrency.setSummary(tableCurrency.getCurrencyName());
                }
                lstBaseCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (currencyUtils.setBaseCurrencyId(Integer.valueOf(String.valueOf(newValue)))) {
                            currencyUtils.reInit();
                            TableCurrencyFormats tableCurrency = currencyUtils.getTableCurrencyFormats(currencyUtils.getBaseCurrencyId());
                            if (tableCurrency != null) {
                                lstBaseCurrency.setSummary(tableCurrency.getCurrencyName());
                            }
                        }
                        return true;
                    }
                });
            }

            // default status
            final ListPreference lstDefaultStatus = (ListPreference) findPreference(PreferencesConstant.PREF_DEFAULT_STATUS);
            if (lstDefaultStatus != null) {
                setSummaryListPreference(lstDefaultStatus, lstDefaultStatus.getValue(), R.array.status_values, R.array.status_items);
                lstDefaultStatus.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        setSummaryListPreference(lstDefaultStatus, newValue.toString(), R.array.status_values, R.array.status_items);
                        return true;
                    }
                });
            }

            //default payee
            final ListPreference lstDefaultPayee = (ListPreference) findPreference(PreferencesConstant.PREF_DEFAULT_PAYEE);
            if (lstDefaultPayee != null) {
                setSummaryListPreference(lstDefaultPayee, lstDefaultPayee.getValue(), R.array.new_transaction_dialog_values, R.array.new_transaction_dialog_items);
                lstDefaultPayee.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        setSummaryListPreference(lstDefaultPayee, newValue.toString(), R.array.new_transaction_dialog_values, R.array.new_transaction_dialog_items);
                        return true;
                    }
                });
            }

            // financial day and month
            final Preference pFinancialDay = findPreference(PreferencesConstant.PREF_FINANCIAL_YEAR_STARTDATE);
            if (pFinancialDay != null) {
                pFinancialDay.setSummary(core.getInfoValue(Constants.INFOTABLE_FINANCIAL_YEAR_START_DAY));
                if (pFinancialDay.getSummary() != null) {
                    pFinancialDay.setDefaultValue(pFinancialDay.getSummary().toString());
                }
                pFinancialDay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int day;
                        try {
                            day = Integer.parseInt((String) newValue);
                            if (!(day >= 1 && day <= 31)) {
                                return false;
                            }
                            if (core.setInfoValue(Constants.INFOTABLE_FINANCIAL_YEAR_START_DAY, Integer.toString(day))) {
                                pFinancialDay.setSummary(Integer.toString(day));
                            }
                            return true;
                        } catch (Exception e) {
                            Log.e(LOGCAT, e.getMessage());
                        }
                        return false;
                    }
                });
            }

            final ListPreference lstFinancialMonth = (ListPreference) findPreference(PreferencesConstant.PREF_FINANCIAL_YEAR_STARTMONTH);
            if (lstFinancialMonth != null) {
                lstFinancialMonth.setEntries(core.getListMonths());
                lstFinancialMonth.setEntryValues(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"});
                lstFinancialMonth.setDefaultValue("0");
                // get current month
                try {
                    String currentMonth = core.getInfoValue(Constants.INFOTABLE_FINANCIAL_YEAR_START_MONTH);
                    if ((!TextUtils.isEmpty(currentMonth)) && NumberUtils.isNumber(currentMonth)) {
                        int month = Integer.parseInt(currentMonth) - 1;
                        if (month > -1 && month < lstFinancialMonth.getEntries().length) {
                            lstFinancialMonth.setSummary(lstFinancialMonth.getEntries()[month]);
                            lstFinancialMonth.setValue(Integer.toString(month));
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOGCAT, e.getMessage());
                }
                lstFinancialMonth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        try {
                            int value = Integer.parseInt(newValue.toString());
                            if (value > -1 && value < lstFinancialMonth.getEntries().length) {
                                if (core.setInfoValue(Constants.INFOTABLE_FINANCIAL_YEAR_START_MONTH, Integer.toString(value + 1))) {
                                    lstFinancialMonth.setSummary(lstFinancialMonth.getEntries()[value]);
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(LOGCAT, e.getMessage());
                            return false;
                        }
                        return true;
                    }
                });
            }
        }

        public void setSummaryListPreference(Preference preference, String value, int idArrayValues, int idArrayItems) {
            final String[] values = getResources().getStringArray(idArrayValues);
            final String[] items = getResources().getStringArray(idArrayItems);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    preference.setSummary(items[i]);
                }
            }
        }

        private String getDateFormatFromMask(String mask) {
            if (!TextUtils.isEmpty(mask)) {
                for (int i = 0; i < getResources().getStringArray(R.array.date_format_mask).length; i++) {
                    if (mask.equals(getResources().getStringArray(R.array.date_format_mask)[i])) {
                        return getResources().getStringArray(R.array.date_format)[i];
                    }
                }
            }
            return null;
        }
    }
}