package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.AcademyCategory
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class AcademyRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var academies by mutableStateOf(loadAcademies())
        private set

    fun addAcademy(
        name: String,
        crestUri: String,
        mainVenue: String,
        description: String,
        categories: List<AcademyCategory>,
    ) {
        academies =
            academies +
                Academy(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    crestUri = crestUri,
                    mainVenue = mainVenue.trim(),
                    description = description.trim(),
                    categories = categories.map { it.copy(name = it.name.trim()) },
                )
        persistAcademies()
    }

    private fun loadAcademies(): List<Academy> {
        val saved = preferences.getString(ACADEMIES_KEY, null) ?: return emptyList()
        val academyArray = JSONArray(saved)
        return buildList {
            for (academyIndex in 0 until academyArray.length()) {
                val academyObject = academyArray.getJSONObject(academyIndex)
                val categoryArray = academyObject.getJSONArray("categories")
                val categories = buildList {
                    for (categoryIndex in 0 until categoryArray.length()) {
                        val categoryObject = categoryArray.getJSONObject(categoryIndex)
                        add(
                            AcademyCategory(
                                name = categoryObject.getString("name"),
                                coachName = categoryObject.getString("coachName"),
                            )
                        )
                    }
                }
                add(
                    Academy(
                        id = academyObject.getString("id"),
                        name = academyObject.getString("name"),
                        crestUri = academyObject.getString("crestUri"),
                        mainVenue = academyObject.getString("mainVenue"),
                        description = academyObject.getString("description"),
                        categories = categories,
                    )
                )
            }
        }
    }

    private fun persistAcademies() {
        val academyArray = JSONArray()
        academies.forEach { academy ->
            val categoryArray = JSONArray()
            academy.categories.forEach { category ->
                categoryArray.put(
                    JSONObject().put("name", category.name).put("coachName", category.coachName)
                )
            }
            academyArray.put(
                JSONObject()
                    .put("id", academy.id)
                    .put("name", academy.name)
                    .put("crestUri", academy.crestUri)
                    .put("mainVenue", academy.mainVenue)
                    .put("description", academy.description)
                    .put("categories", categoryArray)
            )
        }
        preferences.edit().putString(ACADEMIES_KEY, academyArray.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "sportpro_academies"
        const val ACADEMIES_KEY = "academies"
    }
}
