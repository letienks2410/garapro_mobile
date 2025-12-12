package com.example.garapro.ui.feedback

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.CreateFeedbackRequest
import com.example.garapro.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RatingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPAIR_ORDER_ID = "extra_repair_order_id"
        const val RESULT_FEEDBACK_POSTED = 101   // <- return result here
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        val repairOrderId = intent.getStringExtra(EXTRA_REPAIR_ORDER_ID)

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val commentEdit = findViewById<EditText>(R.id.edtFeedback)
        val submitBtn = findViewById<Button>(R.id.btnSubmit)

        submitBtn.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = commentEdit.text.toString().trim()
            val repairOrderId = intent.getStringExtra(RatingActivity.EXTRA_REPAIR_ORDER_ID)
                ?: return@setOnClickListener

            if (rating <= 0) {
                Toast.makeText(this, "Please give a rating.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CreateFeedbackRequest(
                description = comment,
                rating = rating,
                repairOrderId = repairOrderId
            )

            // Show loading UI if needed
            lifecycleScope.launch {
                try {
                    // Call API on IO dispatcher
                    val resp = withContext(Dispatchers.IO) {
                        RetrofitInstance.RepairProgressService.createFeedback(request)
                    }

                    // Back to Main thread to update UI
                    Toast.makeText(
                        this@RatingActivity,
                        "Feedback submitted. Thank you!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Return result so the caller can refresh the list
                    setResult(RESULT_FEEDBACK_POSTED)
                    finish()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@RatingActivity,
                        "Submission failed: $e",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
