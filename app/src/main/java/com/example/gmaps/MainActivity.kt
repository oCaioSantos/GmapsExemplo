package com.example.gmaps

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.gmaps.api.NearbySearchCallback
import com.example.gmaps.api.Rest
import com.example.gmaps.databinding.ActivityMainBinding
import com.example.gmaps.models.Place
import com.example.gmaps.services.MapsService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val repository by lazy {
        Rest.getInstance().create(MapsService::class.java)
    }

    private lateinit var map: GoogleMap
    private lateinit var coords: LatLng

    /*
        Códigos que esperamos passar na requisição
        de permissão;

        Caso a requisição acione o callback (ver linha 241)
        e retorne na propriedade "requestCode" esse valor,
        quer dizer que o usuário autorizou.
    * */
    private val REQUEST_LOCATION_PERMISSIONS_CODE = 100

    /*
        A anotação aqui serve para desabilitar o warning do sistema
        sobre o uso indiscriminado de "OnTouchListener";

        Explicação sucinta: o Android se preocupa com a acessibilidade
        e nisso a arquitetura do sistema entende que não é bom quando
        nós configuramos o "escutador de touch" em um elemento, pois
        ele já gerencia isso e o faz se preocupando com usuários com
        deficiências visuais.

        A solução para isso - além da anotação que ignora o problema -
        seria implementar uma "custom view" que extende TextView
        e implementar a função performClick() - mas isso fugiria do
        nosso escopo aqui.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(
            supportFragmentManager
                .findFragmentById(R.id.mapsFragment) as SupportMapFragment
        ) {
            getMapAsync(this@MainActivity)
        }

        binding.editSearch.setOnTouchListener { _, event ->
            dispatchSearch(event)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!locationServiceIsEnabled()) {
            showLocationRequestDialog()
        } else {
            checkForLocationPermissions()
        }

        if (!networkServiceIsEnabled()) {
            showNetworkRequestDialog()
        }
    }

    /*
        A função registerForActivityResult inicia uma Activity
        para que o usuário interaja e após essa interação a
        função passada como callback aqui será chamada;

        Neste caso - verificaremos se o usuário ativou a
        localização para prosseguir.
    */
    private val locationActivityRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (locationServiceIsEnabled()) {
            checkForLocationPermissions()
        } else {
            showApplicationWontWorkMessage(R.string.gps_required)
        }
    }

    private val networkActivityRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (networkServiceIsEnabled()) {
            checkForLocationPermissions()
        } else {
            showApplicationWontWorkMessage(R.string.network_required)
        }
    }

    private fun locationServiceIsEnabled(): Boolean {
        with(getSystemService(Context.LOCATION_SERVICE) as LocationManager) {
            return LocationManagerCompat.isLocationEnabled(this)
        }
    }

    private fun showApplicationWontWorkMessage(@StringRes message: Int) {
        Toast.makeText(
            baseContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun checkForLocationPermissions() {
        if (!permissionsGranted(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    ACCESS_COARSE_LOCATION,
                    ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSIONS_CODE
            )
        } else {
            getActualLocation()
        }
    }

    private fun showRequestDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes negativeButtonText: Int,
        positiveButtonHandler: () -> Unit
    ) {
        AlertDialog
            .Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                showApplicationWontWorkMessage(negativeButtonText)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                positiveButtonHandler()
            }
            .show()
    }

    private fun requestTurnOnNetwork() {
        networkActivityRequest.launch(Intent(Settings.ACTION_SETTINGS))
    }

    private fun showNetworkRequestDialog() {
        showRequestDialog(
            R.string.network_off,
            R.string.turn_on_network,
            R.string.network_required,
            ::requestTurnOnNetwork
        )
    }

    private fun requestTurnOnLocation() {
        locationActivityRequest.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun showLocationRequestDialog() {
        showRequestDialog(
            R.string.gps_off,
            R.string.turn_on_gps,
            R.string.gps_required,
            ::requestTurnOnLocation
        )
    }

    @SuppressLint("MissingPermission")
    private fun getActualLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                /* Se o usuário desligou a localização
                   o objeto "location" será null;
                */
                if (location != null) {
                    /* Libera a visualização de onde o usuário está, no mapa. */
                    map.isMyLocationEnabled = true

                    coords = LatLng(location.latitude, location.longitude)
                    setMapCameraToActualLocation()
                } else {
                    showLocationRequestDialog()
                }
            }
    }

    /*
        Callback que será chamado após o usuário interagir com
        as requisições de permissão;

        Se o usuário respondeu afirmativamente (ou seja, permitiu),
        obtemos a localização atual do usuário;
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /*
            Detalhe importante: Sabemos se o usuário respondeu afirmativamente
            para as permissões solicitadas quando o sistema nos retorna um
            requestCode igual ao que passamos lá na chamada desse recurso;
            -> Ver linha 65
         */
        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS_CODE -> getActualLocation()
        }
    }

    /*
        Duas formas (uma para APIs mais recentes e outra
        para as mais antigas) de validação da conectividade
        do dispositivo à rede.
    */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun networkServiceIsEnabled(): Boolean {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

            /*
             Se "getNetworkCapabilities" retornar null ou não tiver
             a capacidade de Internet, o dispositivo não tem acesso
             à rede.
            */
            return networkCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) ?: false
        } else {
            /*
                Se o objeto gerenciador de conectividade não tiver
                a informação da rede ativa ou não tiver seu tipo definido
                é certo que não há conexão ativa.
             */
            val type = connectivityManager.activeNetworkInfo?.type ?: return false
            return type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE
        }
    }

    private fun dispatchSearch(event: MotionEvent): Boolean {
        /*
               Uma view tem uma lista de drawables chamada "compoundDrawables";
               O valor 2 acessa o terceiro valor dessa lista que se comporta
               de forma semelhante à um "relógio" ->
                   0 -> Esquerda (left);
                   1 -> Topo (top);
                   2 -> Direita (right);
                   3 -> Fundo (bottom);
           * */
        val DRAWABLE_RIGHT = 2
        if (event.action == MotionEvent.ACTION_UP) {
            /*
                Descontando o padding right do EditText.
            */
            val VIEW_RIGHT = binding.editSearch.right - binding.editSearch.paddingRight
            /*
                Descobrindo a posição do ícone da lupa dentro do EditText;
                A posição real do drawable (lupa) é: A direita da minha EditText menos a largura do ícone.
            * */
            val DRAWABLE_POSITION =
                VIEW_RIGHT - binding.editSearch.compoundDrawables[DRAWABLE_RIGHT].bounds.width()
            /*
                Se o valor de "x" do evento (toque, ou clique como preferir) for maior ou igual
                à posição do ícone da lupa, disparamos nossa função "doSearch";
            * */
            if (
                event.rawX >= DRAWABLE_POSITION
            ) {
                doSearch()
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        map = gMap.apply {
            /* Ao clicar no mapa, em qualquer lugar, escondemos o teclado. */
            setOnMapClickListener {
                hideKeyboard()
            }
        }
    }

    private fun setMapCameraToActualLocation() {
        map.moveCamera(CameraUpdateFactory.newLatLng(coords))
        map.setMinZoomPreference(15.0f)
    }

    private fun permissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun removeAllMarkers() = map.clear()

    private fun addMarkers(places: List<Place>) {
        removeAllMarkers()
        places.forEach { place ->
            with(place.geometry.location) {
                val location = LatLng(lat, lng)
                map.addMarker(MarkerOptions().position(location).title(place.name))
            }
        }
    }

    private fun handleError(errorMessage: String?) {
        errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }

    private fun LatLng.getUrlLike(): String {
        return "${this.latitude},${this.longitude}"
    }

    private fun doSearch() {
        hideKeyboard()
        val apiKey = resources.getString(R.string.MAPS_API_KEY)
        repository.findNearbyPlaces(
            coords.getUrlLike(),
            2000,
            binding.editSearch.text.toString(),
            apiKey
        ).enqueue(NearbySearchCallback(::addMarkers, ::handleError))
    }

    private fun hideKeyboard() {
        /*
            Função necessária para fechar o teclado automaticamente;
            "getSystemService" retorna um gerenciador de serviços genérico
            do sistema, podendo ser sobre o teclado ativo, bluetooth, alarmes,
            bateria do celular, entre outros.

            Como todos os serviços herdam de uma super classe "Object" aqui
            nós faremos um cast para o tipo específico que gerencia os meios
            de entradas de dados no sistema (nesse caso, o teclado), para
            ter acesso à função "hideSoftInputFromWindow" passando o "windowToken",
            que identificada a janela em que o teclado está sendo executado.
         */
        with(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager) {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

}