//References:
//- OpenStreetMap, Tile usage policy: https://operations.osmfoundation.org/policies/tiles/

package org.setu.margadh.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.setu.margadh.AppData
import org.setu.margadh.R
import timber.log.Timber

class MapPickerActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null
    private var markerTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The default packageName user agent got 403 "Access blocked" tiles from OSM, so I changed it to a unique one
        Configuration.getInstance().userAgentValue = "Margadh-NathanBagge/1.0"

        enableEdgeToEdge()
        setContentView(R.layout.activity_map_picker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setTitle(R.string.map_picker_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        markerTitle = intent.getStringExtra("name")
            ?.takeIf { it.isNotBlank() } ?: getString(R.string.selected_location)

        showOtherMarkets(intent.getLongExtra("id", -1L))

        if (intent.hasExtra("lat") && intent.hasExtra("lng")) {
            // If editing, or coordinates already typed, open on that spot
            val start = GeoPoint(
                intent.getDoubleExtra("lat", 0.0),
                intent.getDoubleExtra("lng", 0.0)
            )
            map.controller.setZoom(16.0)
            map.controller.setCenter(start)
            setSelectedPoint(start)
        } else {
            // New market: show the full Map of Ireland
            map.controller.setZoom(7.0)
            map.controller.setCenter(GeoPoint(53.4, -7.9))
        }

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                setSelectedPoint(point)
                return true
            }

            override fun longPressHelper(point: GeoPoint): Boolean {
                setSelectedPoint(point)
                return true
            }
        }
        // Adding the tap overlay last put it on top, so tapping a pin moved it instead of showing its title.
        // Index 0 puts it at the bottom, so markers get their taps first
        map.overlays.add(0, MapEventsOverlay(receiver))

        findViewById<Button>(R.id.confirmLocationButton).setOnClickListener {
            val point = selectedPoint
            if (point == null) {
                Toast.makeText(this, R.string.tap_map_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Timber.i("Location chosen: ${point.latitude}, ${point.longitude}")
            val result = Intent()
                .putExtra("lat", point.latitude)
                .putExtra("lng", point.longitude)
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun setSelectedPoint(point: GeoPoint) {
        selectedPoint = point

        if (marker == null) {
            marker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
        }

        marker?.position = point
        marker?.title = markerTitle
        map.invalidate()
    }

    // Other saved markets shown as stars. The market being edited is skipped, otherwise its
    // saved position sits under the pin, and it's unclear which marker is the new selection
    private fun showOtherMarkets(editingId: Long) {
        AppData.markets.findAll()
            .filter { it.id != editingId }
            .forEach { market ->
                val star = Marker(map).apply {
                    position = GeoPoint(market.lat, market.lng)
                    title = market.name
                    snippet = market.county
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(
                        this@MapPickerActivity,
                        android.R.drawable.star_big_on
                    )
                }
                map.overlays.add(star)
            }
        map.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        map.onPause()
        super.onPause()
    }
}