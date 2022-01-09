# gti-monitor

グリッドタイインバーターから30秒毎に発電量を取得して、グラフ化するプログラムです。  
(This program is get power-generation from "Grid Tie Inverver" every 30sec, and graphed)

![SpringBoot](https://img.shields.io/badge/SpringBoot-2.5.6-green.svg) 
![Lombok](https://img.shields.io/badge/Lombok-1.18.20-green.svg) 
![j2mod](https://img.shields.io/badge/j2mod-2.7.0-green.svg)

TODO イメージ図

## 必要要件 (Requirement)

- グリッドタイインバーター (Grid Tie Inverter)
  - SUN 1000GTIL2
- Java 8 以降 (Java 8 or higher)

## 使い方 (Usage)

1. GTIの通信ポートと、PCまたはRaspberry PIを接続してください。  
   (Connect GTI Serial Port to PC or Raspberry PI)

2. application.yml を編集して、GTIとAmbientの接続情報を記入してください。  
   (Edit application.yml and fills connect information of GTI and Ambient)

3. コンパイル&パッケージング (Compile & Packaging)

    ```command
    mvn clean package
    ```

4. jarとapplication.ymlファイルを同一フォルダに置いて、PCまたはRaspberry PI上で実行  
   (Put jar and application.yml files in same folder, Execute on PC or Raspberry PI)

     ```command
     java -jar gti-monitor-1.0.jar
     ```

## 参考情報 (Appendix)

Ambient Channel Setting  
TODO チャネル設定

Ambient Chart  
TODO チャート
