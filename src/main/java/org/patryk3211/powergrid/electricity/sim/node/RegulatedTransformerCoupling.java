/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.patryk3211.powergrid.electricity.sim.node;

import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;

import java.util.List;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;


/**
 * 安定化電源内部専用の可変変圧器モデル。
 *
 * <p>
 * 外部に変圧器ブロックを配置せず、
 * StabilizedPowerSupplyBlockEntity の内部に
 * 変圧器を1つ保持するためのモデル。
 * </p>
 *
 * <p>
 * 安定化電源は入力電圧と設定出力電圧から
 * secondaryTurns を変更することで出力電圧を調整する。
 * </p>
 *
 * <p>
 * この変圧器には ThermalBehaviour を持たせない。
 * したがって、安定化電源内部の巻線は熱による
 * 劣化・焼損・温度上昇を発生させない。
 * </p>
 *
 * <p>
 * 変圧器比：
 *
 *     Vsecondary / Vprimary
 *         =
 *     secondaryTurns / primaryTurns
 *
 * </p>
 */
public final class RegulatedTransformerCoupling
        extends TransformerCoupling {

    /*
     * =========================================================
     * 端子
     * =========================================================
     */

    private final IElectricNode primary1;
    private final IElectricNode primary2;

    private final IElectricNode secondary1;
    private final IElectricNode secondary2;


    /*
     * =========================================================
     * 巻数
     * =========================================================
     *
     * primaryTurns は固定。
     *
     * secondaryTurns を変更することで、
     * 安定化電源の出力電圧を制御する。
     */

    private final double primaryTurns;

    private double secondaryTurns;


    /*
     * =========================================================
     * コンストラクタ
     * =========================================================
     */

    public RegulatedTransformerCoupling(
            IElectricNode primary1,
            IElectricNode primary2,
            IElectricNode secondary1,
            IElectricNode secondary2,
            Number primaryTurns,
            Number secondaryTurns
    ) {

        super(
                calculateRatio(
                        primaryTurns,
                        secondaryTurns
                ),
                0.0f,
                List.of(
                        primary1,
                        primary2,
                        secondary1,
                        secondary2
                )
        );


        double primary =
                primaryTurns.doubleValue();

        double secondary =
                secondaryTurns.doubleValue();


        /*
         * -----------------------------------------------------
         * 巻数チェック
         * -----------------------------------------------------
         */

        if (
                !Double.isFinite(primary)
                        ||
                        primary <= 0.0
        ) {

            throw new IllegalArgumentException(
                    "primaryTurns must be positive"
            );
        }


        if (
                !Double.isFinite(secondary)
                        ||
                        secondary <= 0.0
        ) {

            throw new IllegalArgumentException(
                    "secondaryTurns must be positive"
            );
        }


        this.primary1 =
                primary1;

        this.primary2 =
                primary2;

        this.secondary1 =
                secondary1;

        this.secondary2 =
                secondary2;


        this.primaryTurns =
                primary;

        this.secondaryTurns =
                secondary;
    }


    /*
     * =========================================================
     * Ratio計算
     * =========================================================
     */

    private static float calculateRatio(
            Number primaryTurns,
            Number secondaryTurns
    ) {

        double primary =
                primaryTurns.doubleValue();

        double secondary =
                secondaryTurns.doubleValue();


        if (
                !Double.isFinite(primary)
                        ||
                        primary <= 0.0
        ) {

            throw new IllegalArgumentException(
                    "primaryTurns must be positive"
            );
        }


        if (
                !Double.isFinite(secondary)
                        ||
                        secondary <= 0.0
        ) {

            throw new IllegalArgumentException(
                    "secondaryTurns must be positive"
            );
        }


        double ratio =
                secondary / primary;


        if (
                !Double.isFinite(ratio)
                        ||
                        ratio <= 0.0
        ) {

            throw new IllegalArgumentException(
                    "Invalid transformer ratio"
            );
        }


        return (float) ratio;
    }


    /*
     * =========================================================
     * 巻数 Getter
     * =========================================================
     */

    public double getPrimaryTurns() {

        return primaryTurns;
    }


    public double getSecondaryTurns() {

        return secondaryTurns;
    }


    /**
     * 現在の変圧比。
     *
     * Vout / Vin
     */
    public double getTurnsRatio() {

        return secondaryTurns
                /
                primaryTurns;
    }


    /*
     * =========================================================
     * 二次側巻数変更
     * =========================================================
     *
     * 安定化電源の電圧制御に使用する。
     *
     * 例えば：
     *
     * 入力 250V
     * 出力設定 100V
     *
     * primaryTurns = 100
     *
     * secondaryTurns = 40
     *
     * ratio = 0.4
     *
     * となり、
     *
     * 250V × 0.4 = 100V
     *
     * を目標とする。
     */

    public void setSecondaryTurns(
            double turns
    ) {

        /*
         * 不正値を無視
         */

        if (
                !Double.isFinite(turns)
                        ||
                        turns <= 0.0
        ) {

            return;
        }


        /*
         * 最大値・最小値による異常比率を防止。
         *
         * 0に近い値を許可すると、
         * 極端なアドミタンス変化によって
         * シミュレーションが不安定になる可能性がある。
         */

        final double minimumTurns =
                primaryTurns * 1.0e-6;


        final double maximumTurns =
                primaryTurns * 1.0e6;


        turns =
                Math.max(
                        minimumTurns,
                        Math.min(
                                maximumTurns,
                                turns
                        )
                );


        /*
         * 現在値と同じなら何もしない。
         */

        if (
                Math.abs(
                        turns
                                -
                                secondaryTurns
                )
                        <
                        1.0e-12
        ) {

            return;
        }


        /*
         * 新しい変圧比。
         */

        double newRatio =
                turns
                        /
                        primaryTurns;


        if (
                !Double.isFinite(newRatio)
                        ||
                        newRatio <= 0.0
        ) {

            return;
        }


        /*
         * 変圧比を変更。
         *
         * setRatio() 内で
         * ElectricalNetwork のアドミタンス行列も
         * 同時に更新する。
         */

        setRatio(
                (float) newRatio
        );


        /*
         * 巻数を保存。
         */

        secondaryTurns =
                turns;
    }


    /*
     * =========================================================
     * 回路モデル
     * =========================================================
     */

    @Override
    public void couple(
            IAdmittanceAdder admittance
    ) {

        /*
         * -----------------------------------------------------
         * 理想変圧器
         * -----------------------------------------------------
         *
         * 一次側：
         *
         *   ratio
         *
         * 二次側：
         *
         *   1
         *
         * として扱う。
         *
         * 巻線抵抗は0。
         * 熱モデルも存在しない。
         */

        admittance.add(
                index,
                primary1.getIndex(),
                ratio
        );


        admittance.add(
                index,
                primary2.getIndex(),
                -ratio
        );


        admittance.add(
                index,
                secondary2.getIndex(),
                1.0
        );


        admittance.add(
                index,
                secondary1.getIndex(),
                -1.0
        );


        admittance.add(
                secondary2.getIndex(),
                index,
                1.0
        );


        admittance.add(
                secondary1.getIndex(),
                index,
                -1.0
        );


        admittance.add(
                primary1.getIndex(),
                index,
                ratio
        );


        admittance.add(
                primary2.getIndex(),
                index,
                -ratio
        );


        /*
         * -----------------------------------------------------
         * 数値安定化用 G_MIN
         * -----------------------------------------------------
         *
         * 通常の2P2S変圧器と同様に、
         * 各側に非常に小さい導通を追加する。
         *
         * これは実際の負荷電流を流すためではなく、
         * 数値計算上の浮遊ノード問題を防ぐため。
         */

        admittance.add(
                primary2.getIndex(),
                primary2.getIndex(),
                G_MIN / 2
        );


        admittance.add(
                secondary2.getIndex(),
                secondary2.getIndex(),
                G_MIN / 2
        );


        admittance.add(
                primary2.getIndex(),
                secondary2.getIndex(),
                -G_MIN / 2
        );


        admittance.add(
                secondary2.getIndex(),
                primary2.getIndex(),
                -G_MIN / 2
        );


        admittance.add(
                primary1.getIndex(),
                primary1.getIndex(),
                G_MIN / 2
        );


        admittance.add(
                secondary1.getIndex(),
                secondary1.getIndex(),
                G_MIN / 2
        );


        admittance.add(
                primary1.getIndex(),
                secondary1.getIndex(),
                -G_MIN / 2
        );


        admittance.add(
                secondary1.getIndex(),
                primary1.getIndex(),
                -G_MIN / 2
        );
    }


    /*
     * =========================================================
     * Ratio変更
     * =========================================================
     *
     * 重要：
     *
     * ネットワークを再構築せず、
     * 現在のアドミタンス行列を差分更新する。
     */

    @Override
    public void setRatio(
            float newRatio
    ) {

        /*
         * 不正値を拒否。
         */

        if (
                !Float.isFinite(newRatio)
                        ||
                        newRatio <= 0.0f
        ) {

            return;
        }


        /*
         * 現在のratioとの差。
         */

        float change =
                newRatio
                        -
                        this.ratio;


        /*
         * 変化が非常に小さい場合。
         */

        if (
                Math.abs(change)
                        <
                        1.0e-8f
        ) {

            this.ratio =
                    newRatio;

            return;
        }


        /*
         * =====================================================
         * ElectricalNetworkへ差分を反映
         * =====================================================
         *
         * 一次側の係数だけがratioに依存している。
         *
         * 二次側の係数は1のまま。
         */

        if (network != null) {

            /*
             * index -> primary1
             */

            network.alterConductanceMatrix(
                    index,
                    primary1.getIndex(),
                    change
            );


            /*
             * index -> primary2
             */

            network.alterConductanceMatrix(
                    index,
                    primary2.getIndex(),
                    -change
            );


            /*
             * primary1 -> index
             */

            network.alterConductanceMatrix(
                    primary1.getIndex(),
                    index,
                    change
            );


            /*
             * primary2 -> index
             */

            network.alterConductanceMatrix(
                    primary2.getIndex(),
                    index,
                    -change
            );
        }


        /*
         * 最後にratioを更新。
         */

        this.ratio =
                newRatio;
    }


    /*
     * =========================================================
     * 電流取得
     * =========================================================
     *
     * ここでは電流を「設定」しない。
     *
     * 電流はElectricalNetworkの解から決まる。
     *
     * これが重要。
     */

    public double getSecondaryCurrent() {

        /*
         * CouplingNodeの状態値を
         * 二次側電流として扱う。
         */

        double current =
                getStateValue();


        if (!Double.isFinite(current))
            return 0.0;


        return current;
    }


    public double getPrimaryCurrent() {

        /*
         * 理想変圧器：
         *
         * Ip = Is × ratio
         *
         * ただし符号はネットワーク側の
         * 電流方向に依存する。
         *
         * 安定化電源の表示用として絶対値を使用。
         */

        double current =
                getStateValue()
                        *
                        ratio;


        if (!Double.isFinite(current))
            return 0.0;


        return Math.abs(current);
    }


    /*
     * =========================================================
     * 出力電流取得
     * =========================================================
     *
     * 旧コードとの互換性用。
     *
     * 「setOutputCurrent()」は実装しない。
     *
     * 出力電流は負荷によって決まる。
     */

    public double getOutputCurrent() {

        return getSecondaryCurrent();
    }
}