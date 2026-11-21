//References:
//- OpenStreetMap, Tile usage policy: https://operations.osmfoundation.org/policies/tiles/
//- osmdroid BoundingBox API: https://javadoc.io/static/org.osmdroid/osmdroid-android/6.0.0/org/osmdroid/util/BoundingBox.html
//- Stack Overflow, fit all markers with zoomToBoundingBox: https://stackoverflow.com/questions/41489668
//- Stack Overflow, zooming before the map is laid out: https://stackoverflow.com/questions/20608590

package org.setu.margadh.activities

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.setu.margadh.AppData
import org.setu.margadh.R
import org.setu.margadh.models.MarketModel
import timber.log.Timber
import org.osmdroid.util.BoundingBox

class MarketMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var nameText: TextView
    private lateinit var detailsText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var marketImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Same unique user agent as the map picker, so OSM doesn't block the tiles
        Configuration.getInstance().userAgentValue = "Margadh-NathanBagge/1.0"

        enableEdgeToEdge()
        setContentView(R.layout.activity_market_map)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setTitle(R.string.market_map)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        map = findViewById(R.id.marketMap)
        nameText = findViewById(R.id.mapNameText)
        detailsText = findViewById(R.id.mapDetailsText)
        descriptionText = findViewById(R.id.mapDescriptionText)
        marketImage = findViewById(R.id.mapImage)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(CopyrightOverlay(this))

        loadMarkers()
    }

    private fun loadMarkers() {
        val markets = AppData.markets.findAll()

        if (markets.isEmpty()){
            map.controller.setZoom(7.0)
            map.controller.setCenter(GeoPoint(53.4, -7.9))
            nameText.setText(R.string.no_markets)
            Timber.i("Market Map: no markets to show")
            return
        }

        markets.forEach { market ->
            val marker = Marker(map)
            marker.position = GeoPoint(market.lat, market.lng)
            marker.title = market.name
            marker.snippet = market.county
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Tapping a marker fills the details panel; true = click handled
            marker.setOnMarkerClickListener { _, _ ->
                showMarket(market)
                true
            }

            map.overlays.add(marker)
        }

// Zoom so every market fits on screen
        val points = markets.map { GeoPoint(it.lat, it.lng) }

        if (points.size == 1) {
            // A single point has no area to fit, so just centre on it
            map.controller.setZoom(14.0)
            map.controller.setCenter(points.first())
        } else {
            val box = BoundingBox.fromGeoPoints(points)
            // post{} waits until the map has been laid out, before that it has no size to fit the box into
            map.post { map.zoomToBoundingBox(box, false, 100) }
        }

        map.invalidate()
        Timber.i("Market map showing ${markets.size} markets")
    }

    private fun showMarket(market: MarketModel) {
        nameText.text = market.name
        detailsText.text = getString(
            R.string.market_details_format,
            market.county, market.openingDays, market.openingHours
        )
        descriptionText.text = market.description

        val imageUri = market.imageUri?.let { Uri.parse(it) }
        if (imageUri != null) {
            marketImage.setImageURI(imageUri)
        } else {
            marketImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        Timber.i("Selected market id=${market.id} on map")
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

    override fun onDestroy() {
        map.onDetach()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.tileProvider.clearTileCache()
        map.invalidate()
    }
}