package com.heterodain.gtimonitor.device;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fazecast.jSerialComm.SerialPort;
import com.heterodain.gtimonitor.config.DeviceConfig.LightSensor;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 照度センサーデバイス
 */
@Component
@Slf4j
public class LightSensorDevice implements Closeable {

    // シリアルポート
    private SerialPort serial;
    // シリアル入力ストリーム
    private BufferedReader in;

    /**
     * 照度センサーに接続する
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public void connect(LightSensor config) throws IOException {
        log.info("照度センサーに接続します: {}", config);

        serial = SerialPort.getCommPort(config.getComPort());
        serial.setBaudRate(9600);
        serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 200);
        if (!serial.openPort()) {
            throw new IOException("シリアルポート[" + config.getComPort() + "]を開けませんでした。");
        }

        in = new BufferedReader(new InputStreamReader(serial.getInputStream(), StandardCharsets.ISO_8859_1));
    }

    /**
     * 照度を取得する
     * 
     * @return 照度(Lux)
     * @throws IOException
     */
    public Double readLux() throws IOException {
        var command = "GET".getBytes();
        serial.writeBytes(command, command.length);

        var line = in.readLine();
        log.trace("Receive: {}", line);
        return Double.parseDouble(line);
    }

    /**
     * シリアルポートを閉じる
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        if (serial != null && serial.isOpen()) {
            serial.closePort();
        }
    }

}