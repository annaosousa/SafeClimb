package com.example.myapplication.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.QueryResult
import com.example.myapplication.FirstActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAuthBinding
import java.security.MessageDigest


class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var client: AmazonDynamoDBClient

    private var name = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        val username = binding.emailField
        val password = binding.passwordField
        val loginButton = binding.buttonStart

        // Inicializar o provedor de credenciais do Amazon Cognito
        val credentialsProvider = CognitoCachingCredentialsProvider(
            requireContext(),
            "eu-north-1:7c834096-3ddd-4282-a809-4390e3e5686a", // ID do grupo de identidades
            Regions.EU_NORTH_1 // Região
        )

        // Inicializando o cliente DynamoDB com as credenciais
        client = AmazonDynamoDBClient(credentialsProvider)
        client.setRegion(Region.getRegion(Regions.US_EAST_1))

        loginButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                if (fetchDataFromDynamoDB(username.text.toString(),password.text.toString())) {
                    val intent = Intent(
                        context,
                        FirstActivity::class.java
                    )
//                    intent.putExtra("loggedIn","true");
//                    intent.putExtra("name",name);
//                    intent.putExtra("email",username.text.toString());
                    saveCred(name,username.text.toString(),true)
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "Login Failed!", Toast.LENGTH_SHORT).show()
                    username.setText("")
                    password.setText("")
                }
            }
        })

        val newUser = binding.newUser
        newUser.setOnClickListener {
            navController.navigate(R.id.navigation_user_creation, null)
        }

        val continueUnAuth = binding.continueUnAuth
        continueUnAuth.setOnClickListener {
            val intent = Intent(
                context,
                FirstActivity::class.java
            )
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun fetchDataFromDynamoDB(user: String, password: String): Boolean {
        val userValue = AttributeValue().withS(user)

        val request = QueryRequest()
            .withTableName("credentials")
            .withKeyConditionExpression("email = :email")
            .addExpressionAttributeValuesEntry(":email", userValue)

        var authenticated = false

        val t = Thread(object : Runnable {
            override fun run() {
                try {
                    val scanResult: QueryResult = client.query(request)
                    val items = scanResult.items
                    if (items.isNotEmpty() && items.size == 1) {
                        val firstUser = items[0]
                        firstUser?.let {
                            val bankPassword = firstUser["password"]?.s
                            name = firstUser["name"]?.s.toString()
                            if(bankPassword == hash(password))
                                authenticated = true
                        }
                    } else {
                        authenticated = false
                    }
                } catch (e: Exception) {
                    authenticated = false
                }
            }
        })

        t.start()
        t.join()

        return authenticated
    }

    fun hash(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun saveCred(name: String, email: String, isLoggedIn: Boolean): Boolean {
        try {
            val sharedPref = activity?.getSharedPreferences("preferences", Context.MODE_PRIVATE) ?: return false
            with (sharedPref.edit()) {
                putBoolean("user_logged", isLoggedIn)
                putString("user_name", name)
                putString("user_email", email)
                apply()
            }
            return true
        }
        catch (e: Exception)
        {
            return false
        }
    }

}