# gti-monitor

グリッドタイインバーターから30秒毎に発電量を取得して、グラフ化するプログラムです。  
(This program is get power-generation from "Grid Tie Inverver" every 30sec, and graphed)

![SpringBoot](https://img.shields.io/badge/SpringBoot-2.5.6-green.svg) 
![Lombok](https://img.shields.io/badge/Lombok-1.18.20-green.svg) 
![j2mod](https://img.shields.io/badge/j2mod-2.7.0-green.svg)

<img width="1920" alt="image" src="https://user-images.githubusercontent.com/46586035/148751011-55361419-d0e6-4601-af0f-048b33df5d25.png">

[![Video1](https://img.youtube.com/vi/dU6PKDX_2wg/0.jpg)](https://www.youtube.com/watch?v=dU6PKDX_2wg) (use v1.0)

## 必要要件 (Requirement)

- グリッドタイインバーター (Grid Tie Inverter)
  - SUN 1000GTIL2-LCD
- Java 8 以降 (Java 8 or higher)

## 使い方 (Usage)

1. GTIの通信ポートと、PCまたはRaspberry PIを接続してください。  
   (Connect GTI Serial Port to PC or Raspberry PI)

2. application.yml を編集して、GTIとAmbientとOpenWeatherの接続情報を記入してください。  
   (Edit application.yml and fills connect information of GTI and Ambient, Open Weather)

3. コンパイル&パッケージング (Compile & Packaging)

    ```command
    mvn clean package
    ```

4. jarとapplication.ymlファイルを同一フォルダに置いて、PCまたはRaspberry PI上で実行  
   (Put jar and application.yml files in same folder, Execute on PC or Raspberry PI)

     ```command
     java -jar gti-monitor-1.1.jar
     ```

## 参考情報 (Appendix)

Ambient Channel Setting  
<img width="902" alt="setting" src="https://user-images.githubusercontent.com/46586035/153027010-a3815c3a-8857-49cd-b237-47ab3c4a214f.png">

Ambient Chart  
<img width="693" alt="chart" src="https://user-images.githubusercontent.com/46586035/153027021-9efdf837-3679-4f77-803d-b0a60d8ff2cf.png">
