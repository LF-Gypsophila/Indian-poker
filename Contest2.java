import java.lang.Math;
import java.util.Arrays; // 中央値・統計分析用

public class KnowledgeClass {

    InfoClass current = new InfoClass(); // ゲーム情報
    InfoClass previous = new InfoClass(); // 直前のゲーム情報

    // 過去10000回分のゲーム履歴情報
    InfoClass[] history = new InfoClass[10000];

    String decision; // コールorドロップの宣言用
    String bid; // ビッド額宣言用

    int gameCount = 0;

    // --- 新たな作戦管理 ---
    // 0: STRATEGY_POWER, 1: STRATEGY_DEFENCE, 2: STRATEGY_BALANCE
    int strategyType = 0;
    static final int STRATEGY_POWER = 0;
    static final int STRATEGY_DEFENCE = 1;
    static final int STRATEGY_BALANCE = 2;

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

    // --- 戦略切り替え判定 ---
    // ・最初の200回はDEFENCEを使用
    // ・相手のベット1が40％に満たないなら、POWER
    // ・相手のベット1が40%を超えており、かつ2より5ベットのほうが多いならDEFENCE
    // ・相手のベット1が40%超えており、かつ5ベットより2ベットのほうがおおいならBALANCE
    private void checkStrategyType() {
        if (gameCount < 200) {
            strategyType = STRATEGY_DEFENCE;
            return;
        }

        int count1 = 0, count2 = 0, count5 = 0;
        int checkRange = Math.min(200, gameCount);

        for (int i = gameCount - 1; i >= gameCount - checkRange; i--) {
            int obid = history[i].opponent_bid;
            if (obid == 1) count1++;
            if (obid == 2) count2++;
            if (obid == 5) count5++;
        }

        double rate1 = (checkRange > 0) ? (double)count1 / checkRange : 0.0;

        if (rate1 < 0.4) {
            strategyType = STRATEGY_POWER;
        } else {
            if (count5 > count2) {
                strategyType = STRATEGY_DEFENCE;
            } else {
                strategyType = STRATEGY_BALANCE;
            }
        }
    }

    // ビッド数の決定
    public String bid() {

        // ビッドする前にゲーム履歴情報を更新する
        HistoryUpdate();
        gameCount++;

        // --- 作戦切り替えの判定 ---
        checkStrategyType();

        int b = 1; // デフォルトは1ドル

        switch (strategyType) {
            case STRATEGY_DEFENCE:
                // ディフェンス型:
                //　2以上4以下には2をかけて5以上には1 
                if (current.opponent_card >= 2 && current.opponent_card <= 4) {
                    b = 2;
                } else {
                    b = 1;
                }
                break;
            case STRATEGY_BALANCE:
                // バランス型:
                // 2,3のとき5ベット
                if (current.opponent_card == 2 || current.opponent_card == 3) {
                    b = 5;
                }
                // 4,5のとき4ベット
                else if (current.opponent_card == 4 || current.opponent_card == 5) {
                    b = 4;
                }
                // 6,7,8のとき3ベット
                else if (current.opponent_card == 6 || current.opponent_card == 7 || current.opponent_card == 8) {
                    b = 3;
                }
                // 9,10,11のとき2ベット
                else if (current.opponent_card == 9 || current.opponent_card == 10 || current.opponent_card == 11) {
                    b = 2;
                }
                // それ以外は1ベット
                else {
                    b = 1;
                }
                break;
            case STRATEGY_POWER:
            default:
                // パワー型
                if (current.opponent_card >= 2 && current.opponent_card <= 5) {
                    b = 5;
                } else {
                    b = 1;
                }
                break;
        }

        // ビッド額は最低1ドル、最高5ドル、かつ自分・相手の残金を超えないように
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

    // コール or ドロップの決定ルール（履歴から予測・安全重視）
    public String decision() {
        decision = "n"; // 初期化

        // 履歴から自分のカードを予測（平均値＋中央値の加重平均）
        int[] matchedCards = new int[history.length];
        int sumCards = 0;
        int count = 0;

        for (int i = 0; i < history.length; i++) {
            if (current.opponent_bid == history[i].opponent_bid) {
                sumCards += history[i].my_card;
                matchedCards[count] = history[i].my_card;
                count++;
            }
        }

        int pred_avg = 9;
        int pred_med = 9;
        if (count > 0) {
            pred_avg = sumCards / count;
            int[] medArray = Arrays.copyOf(matchedCards, count);
            Arrays.sort(medArray);
            if (count % 2 == 0) {
                pred_med = (medArray[count/2 - 1] + medArray[count/2]) / 2;
            } else {
                pred_med = medArray[count/2];
            }
        }

        // 予測値: 安全重視で中央値*0.7 + 平均値*0.3
        double pred_score = pred_med * 0.7 + pred_avg * 0.3;
        int pred_card = (int)Math.round(pred_score);

        // 相手のカードが予測値より強い場合はドロップ
        if (compareCards(current.opponent_card, pred_card) > 0) {
            decision = "d";
        } else {
            decision = "c";
        }

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
