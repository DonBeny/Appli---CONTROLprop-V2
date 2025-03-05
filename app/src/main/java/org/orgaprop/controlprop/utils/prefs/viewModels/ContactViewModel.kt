package org.orgaprop.controlprop.utils.prefs.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orgaprop.controlprop.utils.prefs.models.Contact
import org.orgaprop.controlprop.utils.prefs.repository.ContactRepository
import org.orgaprop.controlprop.utils.types.Result

class ContactViewModel(private val contactRepository: ContactRepository) : ViewModel() {

    // État pour stocker la liste des contacts
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    // État pour stocker le contact actuel
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact

    // État pour stocker les messages d'erreur
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Récupérer tous les contacts
    fun getAllContacts() {
        viewModelScope.launch {
            contactRepository.getAllContacts().collect { contacts ->
                _contacts.value = contacts
            }
        }
    }

    // Récupérer un contact par son adresse
    fun getContact(address: String) {
        viewModelScope.launch {
            contactRepository.getContact(address).collect { contact ->
                _contact.value = contact
            }
        }
    }

    // Insérer un contact
    fun insertContact(contact: Contact) {
        viewModelScope.launch {
            when (val result = contactRepository.insertContact(contact)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec le nouveau contact
                    _contact.value = contact
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Mettre à jour un contact
    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            when (val result = contactRepository.updateContact(contact)) {
                is Result.Success -> {
                    // Succès : mettre à jour l'état avec le contact mis à jour
                    _contact.value = contact
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

    // Supprimer un contact par son ID
    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            when (val result = contactRepository.deleteContact(contactId)) {
                is Result.Success -> {
                    // Succès : réinitialiser l'état du contact
                    _contact.value = null
                    _errorMessage.value = null
                }
                is Result.Failure -> {
                    // Échec : afficher un message d'erreur
                    _errorMessage.value = result.throwable.message
                }
            }
        }
    }

}
