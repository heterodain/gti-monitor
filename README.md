# gti-monitor

グリッドタイインバーターから30秒毎に発電量を取得して、グラフ化するプログラムです。  
(This program is get power-generation from "Grid Tie Inverver" every 30sec, and graphed)

Hive-apiを利用することで、発電量に応じてグラボのPower Limitを動的制御することも可能です。  
(By using Hive-api, it is also possible to dynamically control the "Power Limit" of the graphics board according to the amount of power generated)

![SpringBoot](https://img.shields.io/badge/SpringBoot-2.5.6-green.svg) 
![Lombok](https://img.shields.io/badge/Lombok-1.18.20-green.svg) 
![j2mod](https://img.shields.io/badge/j2mod-3.1.1-green.svg)

![image](https://user-images.githubusercontent.com/46586035/156319330-8c099f52-ccd8-435d-bcae-aed07b43d51e.png)

[![Video1](https://img.youtube.com/vi/aoAeCjsPBZ8/0.jpg)](https://www.youtube.com/watch?v=aoAeCjsPBZ8)

[![Video2](https://img.youtube.com/vi/dU6PKDX_2wg/0.jpg)](https://www.youtube.com/watch?v=dU6PKDX_2wg) (use v1.0)

[![Video3](https://img.youtube.com/vi/-XozmxAGuGM/0.jpg)](https://www.youtube.com/watch?v=-XozmxAGuGM)

[![Video4](https://img.youtube.com/vi/t2q48zudbes/0.jpg)](https://www.youtube.com/watch?v=t2q48zudbes) (use v1.2)

[![Video5](https://img.youtube.com/vi/VNIPfq1SQAE/0.jpg)](https://www.youtube.com/watch?v=VNIPfq1SQAE)

[![Video6](https://img.youtube.com/vi/P8n4UprHtAU/0.jpg)](https://www.youtube.com/watch?v=P8n4UprHtAU)

## 必要要件 (Requirement)

- USB照度センサーまたはグリッドタイインバーター (USB light sensor or Grid tie inverter)
  - [Light sensor] Arduino + BH1750FVI
  - [GTI] SUN 1000GTIL2-LCD
- Java 11 以降 (Java 11 or higher)

## 使い方 (Usage)

1. GTIの通信ポートと照度センサーを、Hive OSを動作させているPCに接続してください。  
   (Connect the GTI's communication port and the light sensor's USB to a PC running the Hive OS)

2. application.yml を編集して、GTIと照度センサーとAmbientとOpenWeatherとHive-apiの接続情報を記入してください。  
   (Edit application.yml and fills connect information of GTI, Light Sensor, Ambient, Open Weather, and Hive-api)

3. jarとapplication.ymlファイルを同一フォルダに置いて、Hive OS上のターミナルで実行してください。  
   (Put jar and application.yml files in same folder, Run in a Terminal on Hive OS)

     ```command
     java -jar gti-monitor-1.3.jar
     ```

## 参考情報 (Appendix)

コンパイル&パッケージング (Compile & Packaging)

    ```command
    mvn clean package
    ```

Ambientチャネル設定 (Ambient Channel Setting)  
<img alt="setting1" src="https://user-images.githubusercontent.com/46586035/171455991-c12de70d-3766-43e8-9f9f-b890977039ad.png">  
<img alt="setting2" src="https://user-images.githubusercontent.com/46586035/171455997-1bc3f1a7-d169-4b42-98b6-d82028de43fe.png">

Ambientグラフイメージ (Ambient Chart Image)  
<img alt="chart" src="https://user-images.githubusercontent.com/46586035/171456781-4bfc29ec-3690-477e-a996-da3ffe7dc909.png">
