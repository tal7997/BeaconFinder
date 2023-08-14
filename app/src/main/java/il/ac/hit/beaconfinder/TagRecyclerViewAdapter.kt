package il.ac.hit.beaconfinder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.databinding.RvTagBinding

class TagRecyclerViewAdapter internal constructor(
    private val context: Context,
    data: List<TagData>
) : RecyclerView.Adapter<TagRecyclerViewAdapter.ViewHolder>() {
    private val mData: ArrayList<TagData> = ArrayList(data)
    private var mClickListener: ItemClickListener? = null

    /**
     * Inflates the row layout from xml when needed
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = RvTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    /**
     * Binds the data to the TextView in each row
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tag = mData[position]
        holder.binding.tvTagName.text = tag.description

        val batteryStatus = tag.getBatteryPercent()
        if (batteryStatus == null) {
            holder.binding.tvTagBattery.text = context.getString(R.string.batteryUnknown)
        } else {
            holder.binding.tvTagBattery.text =
                context.getString(R.string.batteryEstimate, batteryStatus * 100f)
        }

        if (tag.distance.isInfinite() || tag.distance.isNaN()) {
            holder.binding.tvTagDistance.text = context.getString(R.string.distanceUnknown)
        } else {
            holder.binding.tvTagDistance.text =
                context.getString(R.string.distanceEstimate, tag.distance)
        }

        // Read bitmap from media store and load it into the ImageView
        try {
            val uri = Uri.parse(tag.icon)
            // suppress deprecation, when using android < 10 (less than Q)
            // use the old getBitmap, otherwise use decodeBitmap
            @Suppress("DEPRECATION") val bitmap = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                else -> {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
            }
            holder.binding.ivTagIcon.setImageBitmap(bitmap)
        } catch (e: Exception) {
            holder.binding.ivTagIcon.setImageResource(R.drawable.ic_baseline_error_24)
            Log.v("TagRecyclerViewAdapter", "Failed fetching tag image from ${tag.icon}", e)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return Total number of items in the data set held by the adapter.
     */
    override fun getItemCount(): Int {
        return mData.size
    }

    /**
     * ViewHolder object representing a row
     */
    inner class ViewHolder internal constructor(val binding: RvTagBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(view: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION) return

            mClickListener?.onItemClick(view, adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    /**
     * Returns ViewAdapter item by index
     *
     * @param index Index for which to fetch the item
     * @return TagData item at specified index
     */
    fun getItem(index: Int): TagData {
        return mData[index]
    }

    /**
     * Sets item click listener
     */
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    /**
     * Sets ViewAdapter's data
     *
     * @param data Data to set
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<TagData>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    /**
     * Interface to bind a click listener to this ViewAdapter
     */
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }
}
