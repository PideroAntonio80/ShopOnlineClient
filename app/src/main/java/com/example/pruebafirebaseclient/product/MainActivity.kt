package com.example.pruebafirebaseclient.product

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pruebafirebaseclient.Constants
import com.example.pruebafirebaseclient.R
import com.example.pruebafirebaseclient.databinding.ActivityMainBinding
import com.example.pruebafirebaseclient.entities.Product
import com.example.pruebafirebaseclient.profile.ProfileFragment
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.MessageDigest

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val response = IdpResponse.fromResultIntent(it.data)

        if (it.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                Toast.makeText(this, R.string.auth_wellcome, Toast.LENGTH_SHORT).show()
            }
        } else {
            if (response == null) {
                Toast.makeText(this, R.string.auth_see_you, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                response.error?.let {
                    if (it.errorCode == ErrorCodes.NO_NETWORK) {
                        Toast.makeText(this, R.string.error_no_network, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "${R.string.error_code} ${it.errorCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()

        configRecyclerView()
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                //supportActionBar?.title = auth.currentUser?.displayName
                updateTitle(auth.currentUser!!)
                binding.llProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build())

                val loginView = AuthMethodPickerLayout
                    .Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.bEmail)
                    .setGoogleButtonId(R.id.bGoogle)
                    .setFacebookButtonId(R.id.bFacebook)
                    .setPhoneButtonId(R.id.bPhone)
                    .setTosAndPrivacyPolicyId(R.id.tvPolicy)
                    .build()

                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls("https://www.facebook.com", "https://www.facebook.com")
                        .setAuthMethodPickerLayout(loginView)
                        .setTheme(R.style.LoginTheme)
                        .build())
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = getPackageManager().getPackageInfo(
                    "com.example.pruebafirebaseclient",
                    PackageManager.GET_SIGNING_CERTIFICATES)
                for (signature in info.signingInfo.apkContentsSigners) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API >= 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            } else {
                val info = getPackageManager().getPackageInfo(
                    "com.example.pruebafirebaseclient",
                    PackageManager.GET_SIGNATURES);
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API < 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealTime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.rvRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3, GridLayoutManager.HORIZONTAL, false)
            adapter = this@MainActivity.adapter
        }
    }

    /************************************** CIERRE DE SESIÓN **************************************/
    // Botón de "3 puntitos" en la top bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Configuración del botón anterior de los 3 puntitos para cerrar sesión
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.finish_session, Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.llProgress.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this, R.string.error_closing_session, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            R.id.action_profile -> {
                val fragment = ProfileFragment()
                supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment).addToBackStack(null).commit()

                showButton(false)
            }

        }
        return super.onOptionsItemSelected(item)
    }
    /**********************************************************************************************/

    /************************************** LISTADO DE PRODUCTOS DE LA BBDD FIRESTORE EN TIEMPO REAL **************************************/
    private fun configFirestoreRealTime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            for (snapshot in snapshots!!.documentChanges) {
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id
                when(snapshot.type) {
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete(product)
                }
            }
        }
    }
    /**********************************************************************************************/

    override fun onClick(product: Product) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(product: Product) {
        TODO("Not yet implemented")
    }

    override fun updateTitle(user: FirebaseUser) {
        supportActionBar?.title = user.displayName
    }

    override fun showButton(isVisible: Boolean) {
        binding.bViewCart.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}