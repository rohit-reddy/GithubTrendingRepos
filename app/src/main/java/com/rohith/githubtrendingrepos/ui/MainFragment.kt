package com.rohith.githubtrendingrepos.ui

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rohith.githubtrendingrepos.R
import com.rohith.githubtrendingrepos.database.Repo
import com.rohith.githubtrendingrepos.databinding.FragmentMainBinding
import com.rohith.githubtrendingrepos.databinding.ListItemRepoBinding
import com.rohith.githubtrendingrepos.viewmodels.MainViewModel


/**
 * Show a list of repos on screen.
 */
class MainFragment : Fragment() {

    /**
     * One way to delay creation of the viewModel until an appropriate lifecycle method is to use
     * lazy. This requires that viewModel not be referenced before onActivityCreated, which we
     * do in this Fragment.
     */
    private val viewModel: MainViewModel by lazy {
        val activity = requireNotNull(this.activity) {
            "You can only access the viewModel after onActivityCreated()"
        }
        ViewModelProvider(this, MainViewModel.Factory(activity.application))
                .get(MainViewModel::class.java)
    }

    lateinit var listItems : List<Repo>

    /**
     * RecyclerView Adapter for converting a list of repo to cards.
     */
    private var viewModelAdapter: ReposAdapter? = null

    private lateinit var binding: FragmentMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Called immediately after onCreateView() has returned, and fragment's
     * view hierarchy has been created. It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.playlist.observe(viewLifecycleOwner, Observer<List<Repo>> { repos ->
            repos?.apply {
                listItems = repos
                viewModelAdapter?.setItems(repos)
            }
        })
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
         binding = DataBindingUtil.inflate(
                inflater,
                com.rohith.githubtrendingrepos.R.layout.fragment_main,
                container,
                false)
        // Set the lifecycleOwner so DataBinding can observe LiveData
        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = viewModel

        viewModelAdapter = ReposAdapter(ItemClick {
            it, pos ->
            if (viewModel.mPreviousSelectionState != -1 && viewModel.mPreviousSelectionState < listItems.size) {
                listItems[viewModel.mPreviousSelectionState].isSelected = false
                viewModelAdapter?.notifyItemChanged(viewModel.mPreviousSelectionState)
            }
            listItems[listItems.indexOf(it)].isSelected = true
            viewModel.mPreviousSelectionState = listItems.indexOf(it)
            viewModelAdapter?.notifyItemChanged(pos)
        })

        binding.root.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewModelAdapter
        }

        // Observer for the network error.
        viewModel.eventNetworkError.observe(viewLifecycleOwner, { isNetworkError ->
            if (isNetworkError) onNetworkError()
        })

        return binding.root
    }

    /**
     * Method for displaying a Toast error message for network errors.
     */
    private fun onNetworkError() {
        if(!viewModel.isNetworkErrorShown.value!!) {
            Toast.makeText(activity, "Network Error", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem: MenuItem = menu.findItem(R.id.action_search)
        val searchView: SearchView = searchItem.getActionView() as SearchView
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModelAdapter?.filter?.filter(newText)
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }
}

/**
 * Click listener for repos. By giving the block a name it helps a reader understand what it does.
 *
 */
class ItemClick(val block: (Repo, Int) -> Unit) {

    fun onClick(repo: Repo, position: Int) = block(repo, position)
}

/**
 * RecyclerView Adapter for setting up data binding on the items in the list.
 */
class ReposAdapter(val callback: ItemClick) : RecyclerView.Adapter<ReposViewHolder>(), Filterable {

    /**
     * The repos that our Adapter will show
     */
    private lateinit var repos: List<Repo>

    var filterList = mutableListOf<Repo>()

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReposViewHolder {
        val withDataBinding: ListItemRepoBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
            ReposViewHolder.LAYOUT,
                parent,
                false)
        return ReposViewHolder(withDataBinding)
    }

    override fun getItemCount() = filterList.size

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     */
    override fun onBindViewHolder(holder: ReposViewHolder, position: Int) {
        holder.viewDataBinding.also {
            it.repo = filterList[position]
            it.itemCallback = callback
        }
        holder.view.setOnClickListener {
            callback.onClick(filterList[position], position)
        }
    }

    fun setItems(items : List<Repo>) {
        this.repos = items
        this.filterList = repos as MutableList<Repo>
        notifyDataSetChanged()
    }
    override fun getFilter(): Filter {
        return exampleFilter
    }

    private val exampleFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString()
            if (charSearch.isEmpty()) {
                filterList = repos as MutableList<Repo>
            } else {
                val resultList = mutableListOf<Repo>()
                for (row in repos) {
                    if (row.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                        resultList.add(row)
                    }
                }
                filterList = resultList
            }
            val filterResults = FilterResults()
            filterResults.values = filterList
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filterList = results?.values as MutableList<Repo>
            notifyDataSetChanged()
        }
    }

}

/**
 * ViewHolder for Repo items. All work is done by data binding.
 */
class ReposViewHolder(val viewDataBinding: ListItemRepoBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root) {

    val view = viewDataBinding.clickableOverlay
    companion object {
        @LayoutRes
        val LAYOUT = R.layout.list_item_repo
    }
}