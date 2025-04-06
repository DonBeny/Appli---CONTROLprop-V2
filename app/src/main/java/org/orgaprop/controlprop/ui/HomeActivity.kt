package org.orgaprop.controlprop.ui

import android.os.Bundle
import org.orgaprop.controlprop.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeComponents()
        setupComponents()
    }



    override fun initializeComponents() {
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    override fun setupComponents() {
        val userData = getUserData()
        val entrySelected = getEntrySelected()
        val typeCtrl = getTypeCtrl()

        var mess = userData?.idMbr.toString() + " => " + userData?.adrMac

        if( entrySelected != null ) {
            mess += "\nentry : ${entrySelected.id} => ${entrySelected.name}"
        }

        if( typeCtrl != null ) {
            mess += "\ntypeCtrl : $typeCtrl"
        } else {
            mess += "\nLevée de plan d'actions"
        }

        binding.homeActivityTextView.text = mess
    }
    override fun setupListeners() {}
    override fun setupObservers() {}

}
