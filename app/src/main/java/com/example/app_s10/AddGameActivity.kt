package com.example.app_s10

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddGameActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var isEditing = false
    private var gameId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_game)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val game = intent.getParcelableExtra<Game>("game")
        game?.let {
            isEditing = true
            gameId = it.id
            populateForm(it)
        }

        setupViews()
    }

    private fun populateForm(game: Game) {
        findViewById<TextInputEditText>(R.id.etGameTitle).setText(game.title)
        findViewById<TextInputEditText>(R.id.etGameGenre).setText(game.genre)
        findViewById<RatingBar>(R.id.ratingBar).rating = game.rating
    }

    private fun setupViews() {
        val etTitle = findViewById<TextInputEditText>(R.id.etGameTitle)
        val etGenre = findViewById<TextInputEditText>(R.id.etGameGenre)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val btnSave = findViewById<Button>(R.id.btnSaveGame)

        btnSave.text = if (isEditing) "Actualizar Juego" else "Guardar Juego"

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val genre = etGenre.text.toString().trim()
            val rating = ratingBar.rating

            if (validateInput(title, genre)) {
                if (isEditing) {
                    updateGame(title, genre, rating)
                } else {
                    saveGame(title, genre, rating)
                }
            }
        }
    }

    private fun validateInput(title: String, genre: String): Boolean {
        if (title.isEmpty()) {
            showError("El título es obligatorio")
            return false
        }
        if (genre.isEmpty()) {
            showError("El género es obligatorio")
            return false
        }
        return true
    }

    private fun saveGame(title: String, genre: String, rating: Float) {
        val userId = auth.currentUser?.uid ?: return
        val gameId = database.child("games").child(userId).push().key ?: return

        val game = Game(
            id = gameId,
            title = title,
            genre = genre,
            rating = rating,
            userId = userId
        )

        database.child("games").child(userId).child(gameId).setValue(game)
            .addOnSuccessListener {
                showSuccess("¡Juego guardado exitosamente!")
                finish()
            }
            .addOnFailureListener { exception ->
                showError("Error al guardar: ${exception.message}")
            }
    }

    private fun updateGame(title: String, genre: String, rating: Float) {
        val userId = auth.currentUser?.uid ?: return
        val gameId = this.gameId ?: return

        val updatedGame = Game(
            id = gameId,
            title = title,
            genre = genre,
            rating = rating,
            userId = userId
        )

        database.child("games").child(userId).child(gameId).setValue(updatedGame)
            .addOnSuccessListener {
                showSuccess("¡Juego actualizado exitosamente!")
                finish()
            }
            .addOnFailureListener { exception ->
                showError("Error al actualizar: ${exception.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}