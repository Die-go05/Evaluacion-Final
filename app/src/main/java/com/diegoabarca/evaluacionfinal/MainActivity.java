package com.diegoabarca.evaluacionfinal;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText txtCalle, txtCiudad, txtProvincia, txtComuna;
    private ListView lista;
    private Button enviar;
    private FirebaseFirestore db;
//mqtt
    private static String mqttHost = "tcp://mqttserveriot.cloud.shiftr.io:1883";
    private static String idUsuario = "Xiaomi 12T Pro";
    private static String topico = "Mensaje";
    private static String user = "mqttserveriot";
    private static String pass = "3MkYezUtkeX5IcoF";
    private TextView mensajemqtt;

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CargarListaFirestore();
        db = FirebaseFirestore.getInstance();
        txtCalle = findViewById(R.id.editTextCalle);
        txtCiudad = findViewById(R.id.editTextCiudad);
        txtProvincia = findViewById(R.id.editTextProvincia);
        txtComuna = findViewById(R.id.editTextComuna);
        lista = findViewById(R.id.lista);
        mensajemqtt = findViewById(R.id.mensajemqtt);

        try {
            mqttClient = new MqttClient(mqttHost, idUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(user);
            options.setPassword(pass.toCharArray());

            mqttClient.connect(options);
            Toast.makeText(this, "Aplicación conectada al servidor MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexión perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> mensajemqtt.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completa");
                }
            });


        } catch (MqttException e) {


        }
    }

    public void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("datosDirecciones")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> listaDirecciones = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String linea = "|| " + document.getString("calle") + " || " +
                                        document.getString("ciudad") + "|| " +
                                        document.getString("provincia") + "|| " +
                                        document.getString("comuna");
                                listaDirecciones.add(linea);
                            }
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listaDirecciones
                            );
                            lista.setAdapter(adaptador);
                        }
                        else {
                            Log.e("TAG","Error al obtener datos de Firestore", task.getException());
                        }
                    }
                }
        );
    }

    public void enviarDatosFirestore (View view) {

            String calle = txtCalle.getText().toString();
            String ciudad = txtCiudad.getText().toString();
            String provincia = txtProvincia.getText().toString();
            String comuna = txtComuna.getText().toString();

            Map<String, Object> datosDireccion = new HashMap<>();
            datosDireccion.put("calle", calle);
            datosDireccion.put("ciudad", ciudad);
            datosDireccion.put("provincia", provincia);
            datosDireccion.put("comuna", comuna);

            db.collection("datosDirecciones")
                    .document(calle)
                    .set(datosDireccion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Datos enviados a Firestore correctamente", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error al enviar datos a Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });


        try {
            String mensaje = calle;
            if (mqttClient != null && mqttClient.isConnected()){
                mqttClient.publish(topico, mensaje.getBytes(), 0, false);
                mensajemqtt.setText(mensaje);
                Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
            }
        }catch(MqttException e){
            e.printStackTrace();
        }
    }
        public void CargarLista(View view){
            CargarListaFirestore();
    }


}