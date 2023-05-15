package com.example.part2_chapter10.ui.article

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.part2_chapter10.R
import com.example.part2_chapter10.data.ArticleModel
import com.example.part2_chapter10.databinding.FragmentWriteBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

//복습
class WriteArticleFragment : Fragment(R.layout.fragment_write) {

    private lateinit var binding: FragmentWriteBinding

    private var selectedUri: Uri? = null
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedUri = uri
                binding.photoImageView.setImageURI(uri)
                binding.plusButton.isVisible = false
                binding.deleteButton.isVisible = true
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWriteBinding.bind(view)

        startPicker()
        setupPhotoImageView()
        setupDeleteButton()
        setupSubmitButton(view)
        setupBackButton()
    }

    private fun setupDeleteButton() {
        binding.deleteButton.setOnClickListener {
            binding.photoImageView.setImageURI(null)
            selectedUri = null
            binding.deleteButton.isVisible = false
            binding.plusButton.isVisible = true
        }
    }

    private fun setupPhotoImageView() {
        binding.photoImageView.setOnClickListener {

            if (selectedUri == null) {
                startPicker()

            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigate(WriteArticleFragmentDirections.actionBack())

        }
    }

    private fun setupSubmitButton(view: View) {
        binding.submitButton.setOnClickListener {
            showProgress()

            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadImage(
                    uri = photoUri,
                    successHandler = {
                        uploadArticle(it, binding.descriptionEditText.text.toString())
                    },
                    errorHandler = {
                        println(Snackbar.make(view, "이미지 업로드에 실패했습니다.", Snackbar.LENGTH_SHORT))
                        hideProgress()
                    })

            } else {
                Snackbar.make(view, "이미지가 선택되지 않았습니다.", Snackbar.LENGTH_SHORT).show()
                hideProgress()
            }
        }
    }

    private fun startPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }

    private fun showProgress() {
        binding.progressBarLayout.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBarLayout.isVisible = false
    }

    private fun uploadImage(
        uri: Uri,
        successHandler: (String) -> Unit,
        errorHandler: (Throwable?) -> Unit,
    ) {
        val fileName = "${UUID.randomUUID()}.png"
        Firebase.storage.reference.child("articles/photo").child(fileName)
            .putFile(uri)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.storage.reference.child("articles/photo/$fileName")
                        .downloadUrl
                        .addOnSuccessListener {
                            successHandler(it.toString())
                        }.addOnFailureListener {
                            errorHandler(it)
                        }

                } else {
                    errorHandler(task.exception)
                }

            }
    }

    private fun uploadArticle(photoUrl: String, description: String) {
        val articleId = UUID.randomUUID().toString()
        val articleModel = ArticleModel(
            articleId = articleId,
            createdAt = System.currentTimeMillis(),
            description = description,
            imageUrl = photoUrl,
        )

        Firebase.firestore.collection("articles").document(articleId)
            .set(articleModel)
            .addOnSuccessListener {
                findNavController().navigate(WriteArticleFragmentDirections.actionWriteArticleFragmentToHomeFragment())

                hideProgress()
            }.addOnFailureListener{
                it.printStackTrace()
                view?.let { view ->
                    Snackbar.make(view, "글 작성에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                }
                hideProgress()
            }


        hideProgress()

    }
}

//