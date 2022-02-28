/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofill.service.settings

import androidx.annotation.RequiresApi
import android.os.Build
import com.example.android.autofill.service.R
import android.os.Bundle
import android.content.Intent
import android.view.autofill.AutofillManager
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import android.content.DialogInterface
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import android.view.ViewGroup
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.android.autofill.service.data.AutofillDataBuilder
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.FakeAutofillDataBuilder
import com.example.android.autofill.service.data.source.PackageVerificationDataSource
import com.example.android.autofill.service.data.source.local.DefaultFieldTypesLocalJsonSource
import com.example.android.autofill.service.data.source.local.LocalAutofillDataSource
import com.example.android.autofill.service.data.source.local.SharedPrefsPackageVerificationRepository
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.util.AppExecutors
import com.example.android.autofill.service.util.Util

class SettingsActivity : AppCompatActivity() {
    private var mAutofillManager: AutofillManager? = null
    private var mLocalAutofillDataSource: LocalAutofillDataSource? = null
    private var mPackageVerificationDataSource: PackageVerificationDataSource? = null
    private var mPreferences: MyPreferences? = null
    private var mPackageName: String? = null
    @RequiresApi(api = Build.VERSION_CODES.O)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multidataset_service_settings_activity)
        val localAfDataSourceSharedPrefs =
            getSharedPreferences(LocalAutofillDataSource.SHARED_PREF_KEY, MODE_PRIVATE)
        val defaultFieldTypesSource: DefaultFieldTypesLocalJsonSource? =
            DefaultFieldTypesLocalJsonSource.getInstance(
                resources,
                GsonBuilder().create())
        val autofillDao: AutofillDao? = AutofillDatabase.getInstance(
            this, defaultFieldTypesSource, AppExecutors())?.autofillDao()
        mPackageName = packageName
        mLocalAutofillDataSource =
            LocalAutofillDataSource.Companion.getInstance(localAfDataSourceSharedPrefs,
                autofillDao, AppExecutors())
        mAutofillManager = getSystemService(AutofillManager::class.java)
        mPackageVerificationDataSource =
            SharedPrefsPackageVerificationRepository.Companion.getInstance(this)
        mPreferences = MyPreferences.Companion.getInstance(this)
        setupSettingsSwitch(R.id.settings_auth_responses_container,
            R.id.settings_auth_responses_label,
            R.id.settings_auth_responses_switch,
            mPreferences!!.isResponseAuth()
        ) { compoundButton: CompoundButton?, isResponseAuth: Boolean ->
            mPreferences!!.setResponseAuth(isResponseAuth)
        }
        setupSettingsSwitch(R.id.settings_auth_datasets_container,
            R.id.settings_auth_datasets_label,
            R.id.settings_auth_datasets_switch,
            mPreferences!!.isDatasetAuth()
        ) { compoundButton: CompoundButton?, isDatasetAuth: Boolean ->
            mPreferences?.setDatasetAuth(isDatasetAuth)
        }
        setupSettingsButton(R.id.settings_add_data_container,
            R.id.settings_add_data_label,
            R.id.settings_add_data_icon
        ) { view: View? -> buildAddDataDialog()!!.show() }
        setupSettingsButton(R.id.settings_clear_data_container,
            R.id.settings_clear_data_label,
            R.id.settings_clear_data_icon
        ) { view: View? -> buildClearDataDialog()!!.show() }
        setupSettingsButton(R.id.settings_auth_credentials_container,
            R.id.settings_auth_credentials_label,
            R.id.settings_auth_credentials_icon
        ) { view: View? ->
            if (mPreferences!!.getMasterPassword() != null) {
                buildCurrentCredentialsDialog()!!.show()
            } else {
                buildNewCredentialsDialog()!!.show()
            }
        }
        setupSettingsSwitch(R.id.settingsSetServiceContainer,
            R.id.settingsSetServiceLabel,
            R.id.settingsSetServiceSwitch,
            mAutofillManager!!.hasEnabledAutofillServices()
        ) { compoundButton: CompoundButton?, serviceSet: Boolean -> setService(serviceSet) }
        val loggingLevelContainer = findViewById<RadioGroup?>(R.id.loggingLevelContainer)
        val loggingLevel = mPreferences!!.getLoggingLevel()
        Util.setLoggingLevel(loggingLevel)
        when (loggingLevel) {
            Util.LogLevel.Off -> loggingLevelContainer.check(R.id.loggingOff)
            Util.LogLevel.Debug -> loggingLevelContainer.check(R.id.loggingDebug)
            Util.LogLevel.Verbose -> loggingLevelContainer.check(R.id.loggingVerbose)
        }
        loggingLevelContainer.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.loggingOff -> mPreferences!!.setLoggingLevel(
                    Util.LogLevel.Off)
                R.id.loggingDebug -> mPreferences!!.setLoggingLevel(Util.LogLevel.Debug)
                R.id.loggingVerbose -> mPreferences!!.setLoggingLevel(Util.LogLevel.Verbose)
            }
        }
        val dalCheckRequirementContainer =
            findViewById<RadioGroup?>(R.id.dalCheckRequirementContainer)
        val dalCheckRequirement = mPreferences!!.getDalCheckRequirement()
        when (dalCheckRequirement) {
            Util.DalCheckRequirement.Disabled -> dalCheckRequirementContainer.check(R.id.dalDisabled)
            Util.DalCheckRequirement.LoginOnly -> dalCheckRequirementContainer.check(R.id.dalLoginOnly)
            Util.DalCheckRequirement.AllUrls -> dalCheckRequirementContainer.check(R.id.dalAllUrls)
        }
        dalCheckRequirementContainer.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.dalDisabled -> mPreferences!!.setDalCheckRequired(
                    Util.DalCheckRequirement.Disabled)
                R.id.dalLoginOnly -> mPreferences!!.setDalCheckRequired(Util.DalCheckRequirement.LoginOnly)
                R.id.dalAllUrls -> mPreferences!!.setDalCheckRequired(Util.DalCheckRequirement.AllUrls)
            }
        }
    }

    private fun buildClearDataDialog(): AlertDialog? {
        return AlertDialog.Builder(this@SettingsActivity)
            .setMessage(R.string.settings_clear_data_confirmation)
            .setTitle(R.string.settings_clear_data_confirmation_title)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_ok) { dialog: DialogInterface?, which: Int ->
                mLocalAutofillDataSource!!.clear()
                mPackageVerificationDataSource!!.clear()
                mPreferences!!.clearCredentials()
                dialog!!.dismiss()
            }
            .create()
    }

    private fun buildAddDataDialog(): AlertDialog? {
        val numberOfDatasetsPicker = LayoutInflater
            .from(this@SettingsActivity)
            .inflate(R.layout.multidataset_service_settings_add_data_dialog, null)
            .findViewById<NumberPicker?>(R.id.number_of_datasets_picker)
        numberOfDatasetsPicker.minValue = 0
        numberOfDatasetsPicker.maxValue = 10
        numberOfDatasetsPicker.wrapSelectorWheel = false
        return AlertDialog.Builder(this@SettingsActivity)
            .setTitle(R.string.settings_add_data_title)
            .setNegativeButton(R.string.settings_cancel, null)
            .setMessage(R.string.settings_select_number_of_datasets)
            .setView(numberOfDatasetsPicker)
            .setPositiveButton(R.string.settings_ok) { dialog: DialogInterface?, which: Int ->
                val numOfDatasets = numberOfDatasetsPicker.value
                mPreferences!!.setNumberDatasets(numOfDatasets)
                mLocalAutofillDataSource!!.getFieldTypes(object :
                    DataCallback<MutableList<FieldTypeWithHeuristics?>?> {
                    override fun onLoaded(fieldTypes: MutableList<FieldTypeWithHeuristics?>?) {
                        val saved = buildAndSaveMockedAutofillFieldCollections(
                            fieldTypes, numOfDatasets)
                        dialog!!.dismiss()
                        if (saved) {
                            Snackbar.make(findViewById(R.id.settings_layout),
                                resources.getQuantityString(
                                    R.plurals.settings_add_data_success,
                                    numOfDatasets, numOfDatasets),
                                Snackbar.LENGTH_SHORT).show()
                        }
                    }

                    override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
                })
            }
            .create()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun buildAndSaveMockedAutofillFieldCollections(
        fieldTypes: MutableList<FieldTypeWithHeuristics?>?,
        numOfDatasets: Int
    ): Boolean {
        if (numOfDatasets < 0 || numOfDatasets > 10) {
            Util.logw("Number of Datasets (%d) out of range.", numOfDatasets)
        }
        for (i in 0 until numOfDatasets) {
            val datasetNumber = mLocalAutofillDataSource!!.getDatasetNumber()
            val autofillDataBuilder: AutofillDataBuilder =
                FakeAutofillDataBuilder(fieldTypes, mPackageName, datasetNumber)
            val datasetsWithFilledAutofillFields =
                autofillDataBuilder.buildDatasetsByPartition(datasetNumber)
            // Save datasets to database.
            mLocalAutofillDataSource!!.saveAutofillDatasets(datasetsWithFilledAutofillFields)
        }
        return true
    }

    private fun prepareCredentialsDialog(): AlertDialog.Builder? {
        return AlertDialog.Builder(this@SettingsActivity)
            .setTitle(R.string.settings_auth_change_credentials_title)
            .setNegativeButton(R.string.settings_cancel, null)
    }

    private fun buildCurrentCredentialsDialog(): AlertDialog? {
        val currentPasswordField = LayoutInflater
            .from(this@SettingsActivity)
            .inflate(R.layout.multidataset_service_settings_authentication_dialog, null)
            .findViewById<EditText?>(R.id.master_password_field)
        return prepareCredentialsDialog()
            ?.setMessage(R.string.settings_auth_enter_current_password)
            ?.setView(currentPasswordField)
            ?.setPositiveButton(R.string.settings_ok) { dialog, which ->
                val password = currentPasswordField.text.toString()
                if (mPreferences!!.getMasterPassword()
                    == password
                ) {
                    buildNewCredentialsDialog()!!.show()
                    dialog.dismiss()
                }
            }
            ?.create()
    }

    private fun buildNewCredentialsDialog(): AlertDialog? {
        val newPasswordField = LayoutInflater
            .from(this@SettingsActivity)
            .inflate(R.layout.multidataset_service_settings_authentication_dialog, null)
            .findViewById<EditText?>(R.id.master_password_field)
        return prepareCredentialsDialog()
            ?.setMessage(R.string.settings_auth_enter_new_password)
            ?.setView(newPasswordField)
            ?.setPositiveButton(R.string.settings_ok) { dialog: DialogInterface?, which: Int ->
                val password = newPasswordField.text.toString()
                mPreferences?.setMasterPassword(password)
                dialog!!.dismiss()
            }
            ?.create()
    }

    private fun setupSettingsSwitch(
        containerId: Int, labelId: Int, switchId: Int, checked: Boolean,
        checkedChangeListener: CompoundButton.OnCheckedChangeListener?
    ) {
        val container = findViewById<ViewGroup?>(containerId)
        val switchLabel = (container.findViewById<View?>(labelId) as TextView).text.toString()
        val switchView = container.findViewById<Switch?>(switchId)
        switchView.contentDescription = switchLabel
        switchView.isChecked = checked
        container.setOnClickListener { view: View? -> switchView.performClick() }
        switchView.setOnCheckedChangeListener(checkedChangeListener)
    }

    private fun setupSettingsButton(
        containerId: Int, labelId: Int, imageViewId: Int,
        onClickListener: View.OnClickListener?
    ) {
        val container = findViewById<ViewGroup?>(containerId)
        val buttonLabel = container.findViewById<TextView?>(labelId)
        val buttonLabelText = buttonLabel.text.toString()
        val imageView = container.findViewById<ImageView?>(imageViewId)
        imageView.contentDescription = buttonLabelText
        container.setOnClickListener(onClickListener)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setService(enableService: Boolean) {
        if (enableService) {
            startEnableService()
        } else {
            disableService()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun disableService() {
        if (mAutofillManager != null && mAutofillManager!!.hasEnabledAutofillServices()) {
            mAutofillManager!!.disableAutofillServices()
            Snackbar.make(findViewById(R.id.settings_layout),
                R.string.settings_autofill_disabled_message, Snackbar.LENGTH_SHORT).show()
        } else {
            Util.logd("Sample service already disabled.")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun startEnableService() {
        if (mAutofillManager != null && !mAutofillManager!!.hasEnabledAutofillServices()) {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            intent.data = Uri.parse("package:com.example.android.autofill.service")
            Util.logd(TAG, "enableService(): intent=%s", intent)
            startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT)
        } else {
            Util.logd("Sample service already enabled.")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Util.logd(TAG, "onActivityResult(): req=%s", requestCode)
        when (requestCode) {
            REQUEST_CODE_SET_DEFAULT -> onDefaultServiceSet(resultCode)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun onDefaultServiceSet(resultCode: Int) {
        Util.logd(TAG, "resultCode=%d", resultCode)
        when (resultCode) {
            RESULT_OK -> {
                Util.logd("Autofill service set.")
                Snackbar.make(findViewById(R.id.settings_layout),
                    R.string.settings_autofill_service_set, Snackbar.LENGTH_SHORT)
                    .show()
            }
            RESULT_CANCELED -> {
                Util.logd("Autofill service not selected.")
                Snackbar.make(findViewById(R.id.settings_layout),
                    R.string.settings_autofill_service_not_set, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    companion object {
        private val TAG: String? = "SettingsActivity"
        private const val REQUEST_CODE_SET_DEFAULT = 1
    }
}