package com.example.myego.datamodel

import com.google.gson.annotations.SerializedName

data class Pokemons(
    @SerializedName("next")
    val nextPageUrl: String? = null,

    @SerializedName("previous")
    val previousPageUrl: String? = null,

    @SerializedName("count")
    val countPokemonsInAllPages: Int? = null,

    @SerializedName("results")
    val listOfPokemonOverviews: List<PokemonOverview>? = null
)