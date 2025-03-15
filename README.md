EasyAnnouncementでは、jsonも使い駅自動放送を流すことが出来ます。

[使用方法]
1.jsonファイルを作成し、下の例を参考に書きます。
{
  "sounds": [
    {
      "soundId": "easyannouncement:mamonaku",
      "duration": 1
    },
    {
      "soundId": "easyannouncement:($track)",
      "duration": 1
    },
    {
      "soundId": "easyannouncement:($routetype)",
      "duration": 1
    },
    {
      "soundId": "easyannouncement:($boundfor)",
      "duration": 2
    },
    {
      "soundId": "easyannouncement:mairimasu",
      "duration": 1.5
    },
    {
      "soundId": "easyannouncement:kiiroisenn",
      "duration": 1
    }
  ]
}
"sounds"は読み込むために必須の文言です。
ファイル名に関係なく入れて下さい。
"soundId"で流す音声を指定します。
"duration"には、その音声の長さ＋パーツの間隔の数字を入力します。小数対応です。
"soundId"と"duration"を書いたものを複数書くと、上から順番に流れます。

2.記入したjsonファイルを、リソースパックとして読み込むには、<リソースパック名>/assets/easyannouncement/soundsに入れます。

3.リソースパックを適応し、アナウンスブロックを右クリックしGUIを開きます。

4.Select JSONをクリックすると入れているjsonが順番に表示されるので、入れたいものを選び保存します。
同時に、プラットフォーム、秒数も入力します。プラットフォームが空だと流れません。

5.指定時間に放送が流れます。10と入力すると10秒前に流れます。
もし失敗した場合は、「まもなく まいります」とデフォルト放送が流れます。


以上が使い方です。質問があればお願いします。
