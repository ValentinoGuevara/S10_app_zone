package com.example.app_s10

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GamesListActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var chipGroupGenres: ChipGroup
    private lateinit var etSearch: TextInputEditText

    private var allGames = mutableListOf<Game>()
    private var currentSelectedChipId = R.id.chipAll
    private var currentGameSelected: Game? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games_list)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupViews()
        setupRecyclerView()
        loadGames()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.rvGamesList)
        chipGroupGenres = findViewById(R.id.chipGroupGenres)
        etSearch = findViewById(R.id.etSearch)

        // Configurar búsqueda
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterGames()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Configurar chips
        chipGroupGenres.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentSelectedChipId = checkedIds[0]
                filterGames()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        gameAdapter = GameAdapter(
            games = emptyList(),
            onOptionsClicked = { game, view -> showPopupMenu(game, view) }
        )
        recyclerView.adapter = gameAdapter
    }

    private fun loadGames() {
        val userId = auth.currentUser?.uid ?: return

        database.child("games").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allGames.clear()

                    for (gameSnapshot in snapshot.children) {
                        val game = gameSnapshot.getValue(Game::class.java)
                        game?.let { allGames.add(it) }
                    }

                    updateGenreChips()
                    filterGames()
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error al cargar juegos: ${error.message}")
                }
            })
    }

    private fun updateGenreChips() {
        // Obtener todos los géneros únicos
        val genres = allGames.map { it.genre }.distinct()

        // Limpiar chips existentes (excepto el de "Todos")
        chipGroupGenres.removeViews(1, chipGroupGenres.childCount - 1)

        // Añadir chips para cada género
        genres.forEach { genre ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = genre
                isCheckable = true
                setChipBackgroundColorResource(R.color.gaming_pink)
                setChipStrokeColorResource(R.color.gaming_purple)
                chipStrokeWidth = 1f
            }
            chipGroupGenres.addView(chip)
        }

        // Seleccionar el chip "Todos" por defecto
        chipGroupGenres.check(R.id.chipAll)
    }

    private fun filterGames() {
        // 1. Convertir a minúsculas una sola vez (más eficiente)
        val searchQuery = etSearch.text.toString().trim().lowercase()

        // 2. Aplicar ambos filtros en una sola operación (mejor performance)
        val filteredGames = allGames.filter { game ->
            // Filtro por género (si está activo)
            val genreMatch = currentSelectedChipId == R.id.chipAll ||
                    game.genre == chipGroupGenres.findViewById<Chip>(currentSelectedChipId)?.text?.toString()

            // Filtro por título (si hay búsqueda)
            val titleMatch = searchQuery.isEmpty() ||
                    game.title.lowercase().contains(searchQuery)

            genreMatch && titleMatch
        }

        // 3. Actualizar el adaptador
        gameAdapter.updateGames(filteredGames)
    }

    private fun showPopupMenu(game: Game, view: View) {
        currentGameSelected = game
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.game_item_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                currentGameSelected?.let { editGame(it) }
                true
            }
            R.id.menu_delete -> {
                currentGameSelected?.let { deleteGame(it) }
                true
            }
            else -> false
        }
    }

    private fun editGame(game: Game) {
        // Implementar la lógica para editar el juego
        // Puedes abrir AddGameActivity con los datos del juego para editar
        val intent = android.content.Intent(this, AddGameActivity::class.java).apply {
            putExtra("game", game)
        }
        startActivity(intent)
    }

    private fun deleteGame(game: Game) {
        val userId = auth.currentUser?.uid ?: return

        database.child("games").child(userId).child(game.id).removeValue()
            .addOnSuccessListener {
                showToast("Juego eliminado exitosamente")
            }
            .addOnFailureListener { e ->
                showToast("Error al eliminar: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}