package com.example.texttovoice

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.texttospeech.v1beta1.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private val outputFile = "output.mp3"
    private var languageCode: String = "tr-TR" // Varsayılan olarak Türkçe
    private var ssmlGender: SsmlVoiceGender = SsmlVoiceGender.FEMALE // Varsayılan olarak kadın

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //DİL AYARI
        val languages = arrayOf("Türkçe", "İngilizce") // Daha fazla dil ekleyebilirsiniz
        val spinner: Spinner = findViewById(R.id.spinnerLanguage)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                setLanguage(selectedLanguage)
                synthesizeSpeech() // Seçilen dile göre sesi oluştur
            }
        }

        synthesizeSpeech() // İlk yükleme için ses oluştur


        //CİNSİYET AYARI


        val genderOptions = arrayOf("Kadın", "Erkek")
        val spinnerGender: Spinner = findViewById(R.id.spinnerGender)
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter


        spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGender = genderOptions[position]
                setGender(selectedGender)
                synthesizeSpeech() // Seçilen cinsiyete göre sesi oluştur
            }
        }


    }

    fun playSound(view: View) {
        val mediaPlayer = MediaPlayer()
        val file = File(filesDir, getOutputFileName()) // Bu satırı güncelledik

        if (file.exists()) {
            try {
                val filePath = file.absolutePath
                val fis = FileInputStream(file)
                val fd = fis.fd
                mediaPlayer.setDataSource(fd)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: Exception) {
                // Eğer bir şeyler yanlış giderse, bu blok çalışır.
                Toast.makeText(this, "Ses dosyası oynatılamıyor!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            // Dosya mevcut değilse uygun bir hata mesajı gösterebilirsiniz.
            Toast.makeText(this, "Ses dosyası bulunamadı!", Toast.LENGTH_SHORT).show()
        }
    }



    fun setLanguage(language: String) {
        languageCode = when (language) {
            "Türkçe" -> "tr-TR"
            "İngilizce" -> "en-US"
            else -> "en-US"
        }
    }

    fun synthesizeSpeech() {
        val credentials = GoogleCredentials.fromStream(assets.open("inspired-data-395823-8475dae64168.json"))
        val textToSpeechSettings = TextToSpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build()

        val client = TextToSpeechClient.create(textToSpeechSettings)
        val text = "This is a test record"

        val input = SynthesisInput.newBuilder().setText(text.toString()).build()

        val voicesResponse = client.listVoices("")
        val availableVoices = voicesResponse.voicesList.filter { it.languageCodesList.contains(languageCode) }

        val selectedVoice = availableVoices.find { it.ssmlGender == ssmlGender }
        val voiceName = selectedVoice?.name ?: "$languageCode-Standard-A"

        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode(languageCode)
            .setName(voiceName)
            .setSsmlGender(ssmlGender)
            .build()


        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.MP3)
            .build()

        voicesResponse.voicesList.forEach { voice ->
            println("Name: ${voice.name}, Language: ${voice.languageCodesList}, Gender: ${voice.ssmlGender}")
        }

        val response = client.synthesizeSpeech(input, voice, audioConfig)
        val audioContents = response.audioContent

        val file = File(filesDir, outputFile)
        val out: OutputStream = FileOutputStream(file)
        out.write(audioContents.toByteArray())
        out.close()

        client.close()
    }

    fun setGender(gender: String) {
        ssmlGender = when (gender) {
            "Kadın" -> SsmlVoiceGender.FEMALE
            "Erkek" -> SsmlVoiceGender.MALE
            else -> SsmlVoiceGender.FEMALE
        }
    }

    private fun getOutputFileName(): String {
        val editText: EditText = findViewById(R.id.editText)
        val textFromEditText = editText.text.toString().trim()
        return if (textFromEditText.isNotEmpty()) textFromEditText + ".mp3" else "output.mp3"
    }

}
