package com.example.pruebafirebaseclient.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.pruebafirebaseclient.R
import com.example.pruebafirebaseclient.databinding.FragmentProfileBinding
import com.example.pruebafirebaseclient.product.MainAux
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getUser()
        configButtons()
    }

    private fun getUser() {
        binding?.let { binding ->
            FirebaseAuth.getInstance().currentUser?.let { user ->
                binding.etFullName.setText(user.displayName)
                binding.etPhotoUrl.setText(user.photoUrl.toString())

                Glide.with(this)
                    .load(user.photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_access_time)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .circleCrop()
                    .into(binding.ibProfile)

                setupActionBar()
            }
        }
    }

    private fun setupActionBar(){
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.profile_title)
            setHasOptionsMenu(true)
        }
    }

    private fun configButtons() {
        binding?.let { binding ->
            binding.bUpdate.setOnClickListener {
                binding.etFullName.clearFocus()
                binding.etPhotoUrl.clearFocus()
                updateUserProfile(binding)
            }
        }
    }

    private fun updateUserProfile(binding: FragmentProfileBinding) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val profileUpdated = UserProfileChangeRequest.Builder()
                .setDisplayName(binding.etFullName.text.toString().trim())
                .setPhotoUri(Uri.parse(binding.etPhotoUrl.text.toString().trim()))
                .build()

            user.updateProfile(profileUpdated)
                .addOnSuccessListener {
                    Toast.makeText(activity, R.string.user_updated, Toast.LENGTH_SHORT).show()
                    (activity as? MainAux)?.updateTitle(user)
                    activity?.onBackPressed()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, R.string.user_updating_error, Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}