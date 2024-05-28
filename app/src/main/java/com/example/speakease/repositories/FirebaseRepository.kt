package com.example.speakease.repositories

import android.app.Activity
import android.net.Uri
import com.example.speakease.Constants
import com.example.speakease.model.Message
import com.example.speakease.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FirebaseRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun getCurrentUser(onSuccess: (User) -> Unit, onError: (DatabaseError) -> Unit) {
        val currentUserId = auth.uid!!
        database.reference.child(Constants.USERS_PATH).child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let { onSuccess(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error)
                }
            })
    }

    fun getUsers(onSuccess: (List<User>) -> Unit, onError: (DatabaseError) -> Unit) {
        database.reference.child(Constants.USERS_PATH).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user: User = userSnapshot.getValue(User::class.java)!!
                    if (user.uid != auth.uid) users.add(user)
                }
                onSuccess(users)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })
    }

    fun getUserProfileImageReference(): StorageReference {
        return storage.reference.child(Constants.PROFILE_PATH).child(Calendar.getInstance().timeInMillis.toString())
    }

    fun setUserPresenceStatus(status: String) {
        val currentUserId = auth.uid!!
        database.reference.child(Constants.USER_PRESENCE).child(currentUserId)
            .setValue(status)
    }

    fun getChatMessages(senderRoom: String, onSuccess: (List<Message>) -> Unit, onError: (DatabaseError) -> Unit) {
        database.reference.child(Constants.CHATS).child(senderRoom).child(Constants.MESSAGE)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (snapshot1 in snapshot.children) {
                        val message = snapshot1.getValue(Message::class.java)!!
                        message.messageId = snapshot1.key
                        messages.add(message)
                    }
                    onSuccess(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error)
                }
            })
    }

    fun sendMessage(senderRoom: String, receiverRoom: String, message: Message) {
        val randomKey = database.reference.push().key!!
        val date = message.timeStamp
        val lastMsgObj = mapOf("lastMsg" to message.message, "lastMsgTime" to date)

        database.reference.child(Constants.CHATS).child(senderRoom).updateChildren(lastMsgObj)
        database.reference.child(Constants.CHATS).child(receiverRoom).updateChildren(lastMsgObj)

        database.reference.child(Constants.CHATS).child(senderRoom).child(Constants.MESSAGE).child(randomKey)
            .setValue(message)
            .addOnSuccessListener {
                database.reference.child(Constants.CHATS).child(receiverRoom).child(Constants.MESSAGE).child(randomKey)
                    .setValue(message)
            }
    }

    fun getChatImageReference(): StorageReference {
        return storage.reference.child(Constants.CHATS).child(Calendar.getInstance().timeInMillis.toString())
    }

    fun uploadImage(reference: StorageReference, uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        reference.putFile(uri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reference.downloadUrl.addOnCompleteListener { downloadUrlTask ->
                    if (downloadUrlTask.isSuccessful) {
                        val downloadUri = downloadUrlTask.result
                        val imageUrl = downloadUri.toString()
                        onSuccess(imageUrl)
                    } else {
                        onFailure(downloadUrlTask.exception ?: Exception("Unknown error"))
                    }
                }
            } else {
                onFailure(task.exception ?: Exception("Unknown error"))
            }
        }
    }

    fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit,
        onCodeSent: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    onVerificationFailed(exception)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    onCodeSent(verificationId)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun updateUserProfile(name: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.uid!!
        val phone = auth.currentUser?.phoneNumber
        val user = User(uid, name, phone, imageUrl)

        database.reference.child(Constants.USERS_PATH).child(uid)
            .setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun signInWithCredential(
        credential: PhoneAuthCredential,
        onComplete: (Task<AuthResult>) -> Unit
    ) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            onComplete(task)
        }
    }
}