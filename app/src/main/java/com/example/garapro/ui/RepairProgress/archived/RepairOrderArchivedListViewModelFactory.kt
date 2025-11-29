package com.example.garapro.ui.RepairProgress.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.garapro.data.remote.RepairProgressApiService
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository

class RepairOrderArchivedListViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepairOrderArchivedListViewModel::class.java)) {
            val api: RepairProgressApiService = RetrofitInstance.RepairProgressService
            val repository = RepairProgressRepository(api)
            return RepairOrderArchivedListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
