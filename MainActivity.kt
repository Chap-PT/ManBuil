package com.example.mangabuilder

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: PagesAdapter
    private val pages = mutableListOf<Uri>()

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris == null || uris.isEmpty()) return@registerForActivityResult
            uris.forEach { u ->
                contentResolver.takePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                pages.add(u)
            }
            adapter.notifyDataSetChanged()
            rv.scrollToPosition(pages.size - 1)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rv = findViewById(R.id.rvPages)
        adapter = PagesAdapter(pages, contentResolver)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                val item = pages.removeAt(from)
                pages.add(to, item)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
        })
        touchHelper.attachToRecyclerView(rv)

        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            pickImages.launch(arrayOf("image/*"))
        }
        findViewById<android.view.View>(R.id.btnClear).setOnClickListener {
            pages.clear()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Cleared pages", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnExportPdf).setOnClickListener {
            if (pages.isEmpty()) {
                Toast.makeText(this, "No pages to export", Toast.LENGTH_SHORT).show()
            } else {
                exportToPdf()
            }
        }
    }

    private fun exportToPdf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdf = PdfDocument()
                for ((index, uri) in pages.withIndex()) {
                    val input: InputStream? = contentResolver.openInputStream(uri)
                    val bmp = android.graphics.BitmapFactory.decodeStream(input)
                    input?.close()
                    if (bmp == null) continue

                    val pdfWidth = 595
                    val scale = pdfWidth.toFloat() / bmp.width.toFloat()
                    val pdfHeight = (bmp.height * scale).toInt()

                    val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    val scaled = Bitmap.createScaledBitmap(bmp, pdfWidth, pdfHeight, true)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    pdf.finishPage(page)
                    bmp.recycle()
                    scaled.recycle()
                }

                val file = getExternalFilesDir(null)?.resolve("manga_export.pdf")
                if (file == null) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Failed to create file", Toast.LENGTH_SHORT).show() }
                    return@launch
                }
                val fos = FileOutputStream(file)
                pdf.writeTo(fos)
                pdf.close()
                fos.close()

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    val share = Intent(Intent.ACTION_SEND)
                    share.type = "application/pdf"
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@MainActivity,
                        "${packageName}.fileprovider",
                        file
                    )
                    share.putExtra(Intent.EXTRA_STREAM, uri)
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(share, "Share PDF"))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class PagesAdapter(private val pages: List<Uri>, private val cr: android.content.ContentResolver) :
    RecyclerView.Adapter<PagesAdapter.PageVH>() {

    inner class PageVH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
        val iv = v.findViewById<ImageView>(R.id.imgPage)
        return PageVH(iv)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val uri = pages[position]
        holder.iv.load(uri) {
            crossfade(true)
            allowHardware(false)
            scale(Scale.FIT)
        }
        val displayMetrics = holder.iv.context.resources.displayMetrics
        val screenH = displayMetrics.heightPixels
        holder.iv.layoutParams.height = screenH
        holder.iv.requestLayout()
    }

    override fun getItemCount(): Int = pages.size
}
