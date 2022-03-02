# gti-monitor

グリッドタイインバーターから30秒毎に発電量を取得して、グラフ化するプログラムです。  
(This program is get power-generation from "Grid Tie Inverver" every 30sec, and graphed)

Hive-apiを利用することで、発電量に応じてグラボのPower Limitを動的制御することも可能です。  
(By using Hive-api, it is also possible to dynamically control the "Power Limit" of the graphics board according to the amount of power generated)

![SpringBoot](https://img.shields.io/badge/SpringBoot-2.5.6-green.svg) 
![Lombok](https://img.shields.io/badge/Lombok-1.18.20-green.svg) 
![j2mod](https://img.shields.io/badge/j2mod-3.1.1-green.svg)

![image](https://user-images.githubusercontent.com/46586035/156317579-0bb5976a-792b-4e4e-864c-3257308575a9.png)

[![Video1](https://img.youtube.com/vi/dU6PKDX_2wg/0.jpg)](https://www.youtube.com/watch?v=dU6PKDX_2wg) (use v1.0)

## 必要要件 (Requirement)

- グリッドタイインバーター (Grid Tie Inverter)
  - SUN 1000GTIL2-LCD
- Java 11 以降 (Java 11 or higher)

## 使い方 (Usage)

1. GTIの通信ポートと、Hive OSを動作させているPCを接続してください。  
   (Connect GTI Serial Port to PC running the Hive OS)

2. application.yml を編集して、GTIとAmbientとOpenWeatherとHive-apiの接続情報を記入してください。  
   (Edit application.yml and fills connect information of GTI, Ambient, Open Weather, and Hive-api)

3. コンパイル&パッケージング (Compile & Packaging)

    ```command
    mvn clean package
    ```

4. jarとapplication.ymlファイルを同一フォルダに置いて、Hive OS上のターミナルで実行  
   (Put jar and application.yml files in same folder, Run in a Terminal on Hive OS)

     ```command
     java -jar gti-monitor-1.2.jar
     ```

## 参考情報 (Appendix)

Ambient Channel Setting  
<img width="902" alt="setting" src="https://user-images.githubusercontent.com/46586035/155965319-bc29c2c5-9549-414f-8d2e-3be1d382f32c.png">

Ambient Chart  
<img width="693" alt="chart" src="https://user-images.githubusercontent.com/46586035/153027021-9efdf837-3679-4f77-803d-b0a60d8ff2cf.png">
