package edu.temple.audiobb

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import edu.temple.audlibplayer.PlayerService

class MainActivity : AppCompatActivity(), BookListFragment.BookSelectedInterface, ControlFragment.ControlsClickedInterface {

    private lateinit var bookListFragment : BookListFragment

    var isConnected = false
    lateinit var controlsBinder: edu.temple.audlibplayer.PlayerService.MediaControlBinder

    var currentTime: Int = 0

    val progressHandler = Handler(Looper.getMainLooper()){

        if(it.obj != null) {
            val bookProgressObject = it.obj as PlayerService.BookProgress

            val progressTime = bookProgressObject.progress

            var progressTextView = findViewById<TextView>(R.id.progressText)
            progressTextView.text = progressTime.toString()

            var seekBar = findViewById<SeekBar>(R.id.seekBar)
            seekBar.progress = progressTime

        }
        true
    }

    val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isConnected = true
            controlsBinder = service as PlayerService.MediaControlBinder
            controlsBinder.setProgressHandler(progressHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }

    private val searchRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        supportFragmentManager.popBackStack()
        it.data?.run {
            bkListViewModel.copyBooks(getSerializableExtra(BkList.BOOKLIST_KEY) as BkList)
            bookListFragment.bookListUpdated()
        }
    }

    private val isSingleContainer : Boolean by lazy{
        findViewById<View>(R.id.container2) == null
    }

    private val selectedBkViewModel : SelectedBkViewModel by lazy {
        ViewModelProvider(this).get(SelectedBkViewModel::class.java)
    }

    private val bkListViewModel : BkList by lazy {
        ViewModelProvider(this).get(BkList::class.java)
    }

    companion object {
        const val BOOKLISTFRAGMENT_KEY = "BookListFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().add(R.id.controlsContainer, ControlFragment()).commit()

        bindService(Intent(this, PlayerService:: class.java)
            , serviceConnection
            , BIND_AUTO_CREATE)

        if (supportFragmentManager.findFragmentById(R.id.container1) is BkDetailsFragment
            && selectedBkViewModel.getSelectedBook().value != null) {
            supportFragmentManager.popBackStack()
        }

        if (savedInstanceState == null) {
            bookListFragment = BookListFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container1, bookListFragment, BOOKLISTFRAGMENT_KEY)
                .commit()
        } else {
            bookListFragment = supportFragmentManager.findFragmentByTag(BOOKLISTFRAGMENT_KEY) as BookListFragment
                if (isSingleContainer && selectedBkViewModel.getSelectedBook().value != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container1, BkDetailsFragment())
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit()
            }
        }

        if (!isSingleContainer && supportFragmentManager.findFragmentById(R.id.container2) !is BkDetailsFragment)
            supportFragmentManager.beginTransaction()
                .add(R.id.container2, BkDetailsFragment())
                .commit()

        findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            searchRequest.launch(Intent(this, SearchActivity::class.java))
        }

    }

    override fun onBackPressed() {
        selectedBkViewModel.setSelectedBook(null)
        super.onBackPressed()
    }

    override fun bookSelected(book: Book) {

        if (isSingleContainer) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container1, BkDetailsFragment())
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun playClicked(progressTime: Int) {
        val currentBook = selectedBkViewModel.getSelectedBook().value
        if (currentBook != null) {
            Log.d("Progress time before seek to", progressTime.toString())
            if(progressTime > 0){
                controlsBinder.seekTo(progressTime)
            }
            else{
                controlsBinder.play(currentBook.id)
            }
            Log.d("Current book before play", currentBook.toString())
            
        }
    }

    override fun pauseClicked() {
        controlsBinder.pause()
    }

    override fun stopClicked() {
        controlsBinder.stop()
    }

    override fun seekBarClicked() {
        TODO("Not yet implemented")
    }
}