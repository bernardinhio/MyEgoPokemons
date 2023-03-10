package com.example.myego.view

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.map
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myego.R
import com.example.myego.adapter.PokemonsLoadStateAdapter
import com.example.myego.adapter.PokemonsPagingDataAdapter
import com.example.myego.viewmodel.MainFragmentViewModel
import com.example.myego.databinding.FragmentMainBinding
import com.example.myego.datamodel.PokemonDetails
import com.example.myego.datamodel.PokemonOverview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {

    lateinit var viewBinding: FragmentMainBinding
    val viewModel: MainFragmentViewModel by viewModels()
    lateinit var pokemonsPagingDataAdapter: PokemonsPagingDataAdapter

    lateinit var currentPagingData: PagingData<PokemonOverview>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentMainBinding.inflate(inflater)

        setHasOptionsMenu(true)
        return viewBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.fetchData -> {
                viewModel.fetchApiAndUpdatePagingData()
                true
            }
            R.id.reset -> {
                pokemonsPagingDataAdapter.submitData(
                    viewLifecycleOwner.lifecycle,
                    PagingData.empty()
                )
                true
            }
            else -> false
        }

    }

    // Modify the already emited PagingData to add to it new PokemonDetails
    fun onPagerItemClicked5(pokemonId: String, pokemonPosition: Int){
        if (this::currentPagingData.isInitialized){

            var modifiedPokemonDetails: PokemonDetails
            viewModel.pokemonDetailsLiveData.observe(
                viewLifecycleOwner,
                { pokemonDetails ->
                    modifiedPokemonDetails = pokemonDetails

                    var pokemonOverviewIndex: Int = -1

                    // Use map{} to create a list
                    val newPagingData: PagingData<PokemonOverview> = currentPagingData.map { pokemonOverview ->
                        pokemonOverviewIndex++
                        Log.d("pokemonOverviewIndex", pokemonOverviewIndex.toString())
                        if (pokemonOverviewIndex == pokemonPosition){

                            // Toogle close or open call backend
                            if (pokemonOverview.uiIsExpanded == false){
                                pokemonOverview.uiIsExpanded = true

                                // update the clicked pokemonOverview with properties of modified pokemonOverview
                                pokemonOverview.uiPokemonId = pokemonId.toInt()
                                pokemonOverview.uiBaseExperience = modifiedPokemonDetails.baseExperience
                                pokemonOverview.uiHeight = modifiedPokemonDetails.height
                                pokemonOverview.uiWeight = modifiedPokemonDetails.weight
                                pokemonOverview.uiOrder = modifiedPokemonDetails.order
                                pokemonOverview.uiSprites = modifiedPokemonDetails.sprites
                                pokemonOverview.uiDataIsLoading = false

                            }

                        } else { // keep all non-Clicked closed
                            pokemonOverview.uiIsExpanded = false
                        }
                        return@map pokemonOverview
                    }

                    currentPagingData = newPagingData

                    pokemonsPagingDataAdapter.submitData(
                        viewLifecycleOwner.lifecycle,
                        currentPagingData
                    )

                    pokemonsPagingDataAdapter.notifyDataSetChanged() // to close all others

                })

            viewModel.fetchPokemonDetails(pokemonId)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pass Hof for Item click Listener
        pokemonsPagingDataAdapter = PokemonsPagingDataAdapter(
            { pokemonId, pokemonPosition -> onPagerItemClicked5(pokemonId, pokemonPosition) }
        )

        val concatAdapter: ConcatAdapter = pokemonsPagingDataAdapter.withLoadStateFooter(
            footer = PokemonsLoadStateAdapter(actionLambdaForOnClick = { pokemonsPagingDataAdapter.retry() })
        )

        with(viewBinding.rvPokemons) {
            layoutManager = LinearLayoutManager(this.context)
            setHasFixedSize(false)  // IMPORTANT we have to make it FALSE not like Normal RecyclerView!!!

            adapter = concatAdapter
        }

        viewModel.pokemonPagingDataLiveData.observe(
            viewLifecycleOwner,
            { pagingData ->
                pagingData?.let {

                    // Initialize global var
                    currentPagingData = it

                    // update RecyclerVoew
                    pokemonsPagingDataAdapter.submitData(
                        viewLifecycleOwner.lifecycle,
                        currentPagingData
                    )
                }
            }
        )

        pokemonsPagingDataAdapter.addLoadStateListener { combinedLoadState ->

            when (combinedLoadState.source.refresh) {

                is LoadState.Loading -> {
                    Log.d("pagingLog", "addLoadStateListener: source.refresh = Loading")
                    viewBinding.pbProgressOutsideRecyclerView.visibility = View.VISIBLE
                    viewBinding.tvMessageOutsideRecyclerView.visibility = View.VISIBLE
                    viewBinding.tvMessageOutsideRecyclerView.text = "Searching Pokemons..."
                }

                is LoadState.NotLoading -> {
                    Log.d("pagingLog", "addLoadStateListener: source.refresh = NotLoading")
                    viewBinding.tvMessageOutsideRecyclerView.visibility = View.INVISIBLE
                    viewBinding.btnRetryOutsideRecyclerView.visibility = View.INVISIBLE
                    viewBinding.pbProgressOutsideRecyclerView.visibility = View.INVISIBLE

                    // Re-show the RecyclerView after the Error is solved
                    viewBinding.rvPokemons.visibility = View.VISIBLE
                }

                // for ex no Internet or Server broken
                is LoadState.Error -> {
                    Log.d("pagingLog", "addLoadStateListener: source.refresh = Error")
                    // show message and button retry
                    viewBinding.tvMessageOutsideRecyclerView.visibility = View.VISIBLE
                    viewBinding.tvMessageOutsideRecyclerView.text =
                        (combinedLoadState.source.refresh as LoadState.Error).error.message
                    viewBinding.btnRetryOutsideRecyclerView.visibility = View.VISIBLE

                    // retry() is fun of PagingDataAdapter that allows us to retry loading
                    // where we have stopped last time when we got an error for ex
                    viewBinding.btnRetryOutsideRecyclerView.setOnClickListener { pokemonsPagingDataAdapter.retry() }

                    viewBinding.pbProgressOutsideRecyclerView.visibility = View.GONE
                    viewBinding.rvPokemons.visibility = View.INVISIBLE
                }
            }

        }

    }

}