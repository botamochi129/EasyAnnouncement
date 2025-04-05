# EasyAnnouncement ダウンロード
## [1.19.2-Fabric](https://github.com/botamochi129/EasyAnnouncement/releases/tag/1.19.2-Fabric)


EasyAnnouncementでは、jsonも使い駅自動放送を流すことが出来ます。

[使用方法]
1.jsonファイルを作成し、下の例を参考に書きます。

    {
      "sounds": [
        {
          "soundPath": "mamonaku",
          "duration": 1
        },
        {
          "soundPath": "($track)",
          "duration": 2
        },
        {
          "soundPath": "($route)",
          "duration": 1.5
        },
        {
          "soundPath": "($routetype)",
          "duration": 1.7
        },
        {
          "soundPath": "($boundfor)",
          "duration": 2.5
        },
        {
          "soundPath": "mairimasu",
          "duration": 0
        }
      ]
    }

"sounds"は読み込むために必須の文言です。
ファイル名に関係なく入れて下さい。
"soundPath"で流す音声を「パスで」指定します。
これを音声ファイルと同じ階層にいれるなら、音声ファイル名だけで大丈夫です。
"duration"には、その音声の長さ＋パーツの間隔の数字を入力します。小数対応です。
"soundPath"と"duration"を書いたものを複数書くと、上から順番に流れます。
($track)にはホーム名、($routetype)には種別の最後の"|"から後ろの部分、($boundfor)には行き先の最後の"|"から後ろの部分、($route)には路線名の最後の"|"から後ろの部分が代入されます。

2.記入したjsonファイルを、リソースパックとして読み込むには、<リソースパック名>/assets/easyannouncement/soundsに入れます。
また使用するサウンドのsounds.jsonへの記入も忘れずに行って下さい。

3.リソースパックを適応し、アナウンスブロックを右クリックしGUIを開きます。

4.Select JSONをクリックすると入れているjsonが順番に表示されるので、入れたいものを選び保存します。
同時に、プラットフォーム、秒数も入力します。プラットフォームが空だと流れません。

5.指定時間に放送が流れます。10と入力すると10秒前に流れます。
もし失敗した場合は、「まもなく まいります」とデフォルト放送が流れます。


以上が使い方です。質問があればお願いします。
