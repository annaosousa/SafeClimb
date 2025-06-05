package com.utfpr.safeclimb.ui.fragment

import android.annotation.SuppressLint
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
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.QueryResult
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult
import com.utfpr.safeclimb.FirstActivity
import com.utfpr.safeclimb.R
import com.utfpr.safeclimb.databinding.FragmentUserEditBinding
import java.security.MessageDigest


class UserEditFragment : Fragment() {

    private var _binding: FragmentUserEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var client: AmazonDynamoDBClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserEditBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        val name = binding.nameField
        val email = binding.emailField
        val password = binding.passwordField
        val repassword = binding.rePasswordField
        val createButton = binding.buttonCreate

        name.setText(FirstActivity.getName())
        email.setText(FirstActivity.getEmail())

        // Inicializar o provedor de credenciais do Amazon Cognito
        val credentialsProvider = CognitoCachingCredentialsProvider(
            requireContext(),
            "eu-north-1:7c834096-3ddd-4282-a809-4390e3e5686a", // ID do grupo de identidades
            Regions.EU_NORTH_1 // Região
        )

        // Inicializando o cliente DynamoDB com as credenciais
        client = AmazonDynamoDBClient(credentialsProvider)
        client.setRegion(Region.getRegion(Regions.US_EAST_1))

        createButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val userUpdated= updateUserOnDynamo(name.text.toString(), email.text.toString(), password.text.toString(), repassword.text.toString())
                if(userUpdated) {
                    Toast.makeText(context, "User Updated!", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.navigation_home, null)
                }
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun updateUserOnDynamo(name: String,email: String, password: String, repassword: String): Boolean {
        val originalEmailValue = AttributeValue().withS(FirstActivity.getEmail())
        val originalNameValue = AttributeValue().withS(FirstActivity.getName())
        val nameValue = AttributeValue().withS(name)
        val emailValue = AttributeValue().withS(email)
        var passwordValue = AttributeValue()
        var newPassword = false
        if(password != "" || repassword != ""){
            if(password == repassword) {
                passwordValue = AttributeValue().withS(hash(password))
                newPassword = true
            }
            else{
                Toast.makeText(context, "Passwords don't match!", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        var request = QueryRequest()
            .withTableName("credentials")
            .withKeyConditionExpression("email = :email")
            .addExpressionAttributeValuesEntry(":email", originalEmailValue)

        var result = false

        var t = Thread(object : Runnable {
            override fun run() {
                try {
                    val scanResult: QueryResult = client.query(request)
                    val items = scanResult.items

                    if (items.isNotEmpty() && items.size == 1) {
                        val originalUser = items[0]

                        var newEmail = false
                        var newName = false
                        var newEmailAlreadyExists = false
                        if(name != FirstActivity.getName()){
                            newName = true
                        }
                        if(email != FirstActivity.getEmail()) {
                            newEmail = true

                            request = QueryRequest()
                                .withTableName("credentials")
                                .withKeyConditionExpression("email = :email")
                                .addExpressionAttributeValuesEntry(":email", emailValue)

                            try {
                                val scanResult: QueryResult = client.query(request)
                                val items = scanResult.items

                                if (items.isNotEmpty()) {
                                    result = false
                                    newEmailAlreadyExists = true;
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(
                                            context,
                                            "User already exists!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                result = false
                                newEmailAlreadyExists = true;
                                requireActivity().runOnUiThread {
                                    Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                        if(!newEmailAlreadyExists){
                            if(!newPassword)
                                passwordValue = AttributeValue().withS(originalUser["password"]?.s)
                            val itemMap = mutableMapOf<String, AttributeValue>()
                            itemMap["email"] = emailValue
                            itemMap["name"] = nameValue
                            itemMap["password"] = passwordValue

                            if(newEmail || newName)
                            {

                                var deleteRequest = DeleteItemRequest()
                                .withTableName("credentials")
                                .withKey(mapOf(
                                    "email" to originalEmailValue,
                                    "name" to originalNameValue
                                ))

                                var putRequest = PutItemRequest()
                                    .withTableName("credentials")
                                    .withItem(itemMap)

                                try {
                                    val deleteResult: DeleteItemResult? = client.deleteItem(deleteRequest)

                                    try {
                                        val itemResult: PutItemResult = client.putItem(putRequest)
                                        result = true
                                    }
                                    catch (e: Exception) {
                                        result = false
                                        requireActivity().runOnUiThread {
                                            Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                catch (e: Exception){
                                    result = false
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else{
                                var editRequest = UpdateItemRequest()
                                    .withTableName("credentials")
                                    .withKey(mapOf(
                                        "email" to originalEmailValue,
                                        "name" to originalNameValue
                                    ))
                                    .withUpdateExpression("SET #p = :newPasswordValue")
                                    .withExpressionAttributeNames(mapOf(
                                        "#p" to "password"
                                    ))
                                    .withExpressionAttributeValues(mapOf(
                                        ":newPasswordValue" to passwordValue
                                    ))
                                try {
                                    val updateResult: UpdateItemResult = client.updateItem(editRequest)
                                    result = true
                                }
                                catch (e: Exception) {
                                    result = false
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    else{
                        result = false
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    result = false
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
        t.start()
        t.join()

        if(result)
        {
            FirstActivity.setName(name)
            FirstActivity.setEmail(email)
        }

        return result
    }

    fun hash(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}