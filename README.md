# 魔術使い — Iron's Spells Ability Addon

AbilitysgorwMOD向けのForge 1.20.1アドオンです。追加能力「魔術使い」を所持して有効にすると、Iron's Spells 'n Spellbooksで習得済みの魔術を6枠へ登録し、魔力を消費せずに発動できます。

## 対応構成

- Minecraft 1.20.1 / Forge 47.4.10 / Java 17
- AbilitysgorwMOD 1.0.8以降
- Iron's Spells 'n Spellbooks 1.20.1-3.16.3（今回ビルド確認した版）
- Iron's Lib 1.20.1-2.1.0
- Curios API 5系
- Iron's Spellsが必要とするGeckoLib、Player Animatorなど

Iron公式の1.20.1開発資料で案内されている io.redspace Mavenと依存方式を使います。Iron公式のSpellDamageEventが非APIパッケージのSpellDamageSource型を公開しているため、開発コンパイルは同じ3.16.3本体アーティファクトを参照します。Ironのコードやアセットは複製していません。アドオンはIronを依存MODとして呼び出します。

## 操作

1. AbilitysgorwMODで「魔術使い」を入手し、能力一覧キー（本体の既定 K）で能力を有効化します。
2. 既定 J で登録画面を開き、左の枠を選んで右側の魔法書などに登録済みの魔術をクリックします。枠の「解除」は魔術使いのショートカットだけを空にし、Ironの魔法書やスクロールには触れません。
3. 既定 L を押し続けて円形ホイールを開き、マウスを方向へ動かして選び、Lを離して発動します。魔術のMP消費はありません。詠唱、対象条件、詠唱中断、魔術レベル、Iron側の効果処理を使います。
4. Options > Controls > Iron Spell Ability でキーを変更できます。魔術ホイールはキーボードに加えてマウス4/5ボタンも使えます。Minecraftのキー設定で発動キーを Mouse 4 または Mouse 5 に割り当ててください。

候補はIron本家のSpellSelectionManagerが選択可能にする、装備中の魔法書・防具などに登録された習得魔術です。登録・解除・発動時にサーバーでも能力の所持/有効状態と呪文の習得状態を再確認します。アドオン枠の解除は本家の魔法書の内容を変更しません。登録後に魔法書を外しても、習得状態が残っていれば能力枠から発動できます。魔術レベルは、プレイヤーが持つスペルコンテナ（Curios装備を含む）の同じ魔術の最大レベルを参照し、その後に得た強化も発動時に反映します。

魔力を使わないため、Ironが提供するCastSource.COMMANDでサーバー詠唱を開始します。Ironのレベル、詠唱、対象条件、効果、割り込み、クールダウン処理を通しますが、CastSourceの値で独自分岐する他アドオンからは通常のスペルブック詠唱と区別されます。

## クールダウンと強化

- 能力のクールダウンはプレイヤーごと・魔術ごとにAbilitysgorwMODの能力データへ保存し、死亡・再ログインをまたいで保持します。時間はワールド時刻で減ります。
- Ironのクールダウン短縮属性を使って基礎クールダウンを計算し、Iron本来のクールダウンも通常のcast経路から設定します。
- AbilitysgorwMODの既存HUD同期パケットには外部アドオン用の行登録APIがないため、このアドオン自身のHUDに残り時間を表示します。既存本体は変更していません。
- Curios Ability Addonの戦技の護符が付与するStrength効果は、Ironの魔術ダメージにも同じレベルの攻撃力ボーナス（Strength 1段階につき+3）として適用します。既存の確率補正は魔術の威力に流用しません。
- 他のアドオンは SpellAbilityEvents.Cooldown と SpellAbilityEvents.SpellDamage をForge Event Busで監視して、魔術使いのクールダウン・魔術ダメージを調整できます。

## ビルド

teramodGuard のビルド済みクラス/JARを隣接フォルダーとして参照します。まず C:\bluearchiveMODFile\teramodGuard の本体をビルドしてから、このフォルダーで実行します。

    .\gradlew.bat build

JARは build\libs\ironSpellAbilityAddon-0.1.0.jar に生成されます。既存のAbilitysgorwMOD、Iron's Spells 'n Spellbooks、その依存MODと同じ mods フォルダーへ入れてください。アドオンは既存の本体MODやCuriosアドオンのファイルを変更しません。

## 確認

このプロジェクトのビルドではコンパイルとリソース処理を検証します。ゲーム内の発動、描画、サーバー/クライアント接続の実プレイ確認は、対象MOD一式を入れたMinecraft環境で行ってください。


## 自動合格ゲート

開発変更の候補を確認するには、プロジェクトルートで次を実行します。

    .\gradlew.bat verifyAddon

このゲートは依存ライブラリを追加せず、スロット範囲・通信値・クールダウン計算と上限・発動成功時だけクールダウンを確定する条件・Strength補正をJavaで検査します。ソース契約と生成JARのメタデータ、ベースAbilitysgorwMODクラスをJARに同梱していないことも確認します。通常の `check` にも同じゲートが含まれます。

円形UIの見え方、キー長押し/離し、Mouse 4/5、実ワールドでのMP消費、スクロール/魔法書の不変、能力の有効状態、再ログイン後のクールダウンなどは、現在の自動ゲートでは証明できません。未確認のゲーム内挙動が残るJARは候補であり、ライブのmodsフォルダーへ自動コピーしないでください。シナリオ一覧は `acceptance/criteria.json` にあります。
