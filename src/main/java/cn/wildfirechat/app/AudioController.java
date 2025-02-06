package cn.wildfirechat.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ws.schild.jave.*;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RestController
public class AudioController {

    @Value("${wfc.audio.cache.dir}")
    String cacheDirPath;
    private File cacheDir;

    @PostConstruct
    public void init() {
        cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    @GetMapping("amr2mp3")
    public CompletableFuture<ResponseEntity<InputStreamResource>> amr2mp3(@RequestParam("path") String amrUrl) throws FileNotFoundException {

        MediaType mediaType = new MediaType("audio", "mp3");
        String mp3FileName = amrUrl.substring(amrUrl.lastIndexOf('/') + 1) + ".mp3";

        File mp3File = new File(cacheDir, mp3FileName);
        if (mp3File.exists()) {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(mp3File));
            return CompletableFuture.completedFuture(ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + mp3File.getName())
                .contentType(mediaType)
                .contentLength(mp3File.length())
                .body(resource));
        }

        return CompletableFuture.supplyAsync(new Supplier<ResponseEntity<InputStreamResource>>() {
            /**
             * Gets a result.
             *
             * @return a result
             */
            @Override
            public ResponseEntity<InputStreamResource> get() {

                try {
                    amr2mp3(amrUrl, mp3File);
                    InputStreamResource resource = new InputStreamResource(new FileInputStream(mp3File));
                    return ResponseEntity.ok()
//                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + mp3File.getName())
                        .contentType(mediaType)
                        .contentLength(mp3File.length())
                        .body(resource);
                } catch (MalformedURLException e) {
                    System.out.println(amrUrl);
                    e.printStackTrace();
                } catch (EncoderException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return ResponseEntity.status(500).build();
            }
        });
    }

    private static void amr2mp3(String sourceUrl, File target) throws MalformedURLException, EncoderException {
        //Audio Attributes
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(128000);
        audio.setChannels(2);
        audio.setSamplingRate(44100);

        //Encoding attributes
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("mp3");
        attrs.setAudioAttributes(audio);

        //Encode
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(new URL(sourceUrl)), target, attrs);

    }

}
