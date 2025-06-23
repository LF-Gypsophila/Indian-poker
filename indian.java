import java.lang.Math;

public class KnowledgeClass {

    InfoClass current = new InfoClass(); // ゲーム情報
    InfoClass previous = new InfoClass(); // 直前のゲーム情報

    // 過去10000回分のゲーム履歴情報
    InfoClass[] history = new InfoClass[10000];

    String decision; // コールorドロップの宣言用
    String bid; // ビッド額宣言用

    KnowledgeClass() {
        for (int i = 0; i < history.length; i++) {
            history[i] = new InfoClass();
        }
    }

    // カード比較メソッド（“2”は“A”に勝つルールを反映）
    private int compareCards(int card1, int card2) {
        if (card1 == 2 && card2 == 1) return 1;  // card1(2) > card2(A)
        if (card1 == 1 && card2 == 2) return -1; // card1(A) < card2(2)
        if (card1 > card2) return 1;
        if (card1 < card2) return -1;
        return 0; // 同じ強さ
    }

    // 賭け金の決定（条件に応じて書き換え済み）
    public String bid() {

        // 賭ける前にゲーム履歴情報を更新する
        HistoryUpdate();

        int b = 1; // デフォルトは1ドル

        // 相手のカードが2,3,4の時は5ドル、それ以外は1ドル
        if (current.opponent_card == 2 || current.opponent_card == 3 || current.opponent_card == 4 ||current.opponent_card == 5) {
            b = 5;
        } else {
            b = 1;
        }

        // ビッド額は最低1ドル、最高5ドル、かつ自分・相手の残金を超えないようにする
        if (b < 1) b = 1;
        if (b > 5) b = 5;
        if (b > current.opponent_money) b = current.opponent_money;
        if (b > current.my_money) b = current.my_money;

        // 返り値は String クラスで
        bid = Integer.toString(b);

        return bid;
    }

    // 不適切な賭金かチェック（1～5ドルの範囲かつ残金内か）
    public boolean isInvalidBid(int bidAmount) {
        if (bidAmount < 1 || bidAmount > 5) return true;
        if (bidAmount > current.my_money) return true;
        if (bidAmount > current.opponent_money) return true;
        return false;
    }

    // コール or ドロップの決定ルール（元のまま）
    public String decision() {

        decision = "n"; // 初期化

        // 履歴 history から自分のカード mycard を予測する
        int mycard = 9;  // 初期値として予測できない場合は9とする
        int sumCards = 0;
        int count = 0;

        // 現在の相手のビッド額と同じビッド額を、過去に相手が賭けていれば、
        // 自分のカードは、そのときのカードと同じであると予測する。
        for (int i = 0; i < history.length; i++) {
            if (current.opponent_bid == history[i].opponent_bid) {
                sumCards += history[i].my_card;  // 自分のカードを足す
                count++;  // 合計数をカウント
            }
        }

        // 過去のデータがあれば、その平均を計算
        if (count > 0) {
            mycard = sumCards / count;  // 平均値を計算して予測値に設定
        }

        // 予測した mycard よりも相手のカードが強いとドロップ
        if (compareCards(current.opponent_card, mycard) > 0)
            decision = "d";
        else
            decision = "c";

        // 返り値は String クラスで
        return decision;
    }

    // historyに直前のゲーム情報 previous を格納する
    private void HistoryUpdate() {
        for (int i = history.length - 2; i >= 0; i--) {
            history[i + 1] = CopyInfo(history[i]);
        }
        history[0] = CopyInfo(previous);
    }

    // InfoClassのインスタンスをコピーする
    private InfoClass CopyInfo(InfoClass Info) {
        InfoClass tmpInfo = new InfoClass();
        tmpInfo.my_bid = Info.my_bid;
        tmpInfo.my_card = Info.my_card;
        tmpInfo.my_decision = Info.my_decision;
        tmpInfo.my_money = Info.my_money;
        tmpInfo.opponent_bid = Info.opponent_bid;
        tmpInfo.opponent_card = Info.opponent_card;
        tmpInfo.opponent_decision = Info.opponent_decision;
        tmpInfo.opponent_money = Info.opponent_money;
        return tmpInfo;
    }
}
