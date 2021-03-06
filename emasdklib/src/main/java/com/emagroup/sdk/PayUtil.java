package com.emagroup.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayUtil {

    private static final String TAG = "PayUtil";
    private static String mAppId;
    private static EmaPayInfo mPayInfo;


    /**
     * 获取钱包信息
     *
     * @param appId
     * @param uuid
     */
    public static void getWalletSetting(String appId, String sid, String uuid, String appKey, HttpInvoker.OnResponsetListener listener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("app_id", appId);
        params.put("sid", sid);
        params.put("uuid", uuid);
        String sign = UCommUtil.getSign(appId, sid, uuid, appKey);
        params.put("sign", sign);
        new HttpInvoker().postAsync(Url.getPayUrlWalletsSeting(), params, listener);
    }


    /**
     * 获取第三方支付列表(包含钱包支付)
     */
    public static void getPayTrdList(final Context context, final Handler handler) {
        List<String> payTrdList = ConfigManager.getInstance(context).getPayTrdListInfo();
        mAppId = ConfigManager.getInstance(context).getAppId();
        getPayOrRechargeList(context, handler, payTrdList);
    }

    /**
     * 获取充值钱包的第三方支付列表(1.充值时  2.钱包钱不够时出现这个)
     *
     * @param context
     * @return
     */
    public static void getRechargeList(final Context context, final Handler handler) {
        List<String> payTrdList = ConfigManager.getInstance(context).getPayTrdListInfo();
        mAppId = ConfigManager.getInstance(context).getAppId();
        //给钱包充值不能自己给自己充值。。。需要排除掉钱包
        String wallet = "wallet";
        if (payTrdList.contains(wallet)) {
            payTrdList.remove(wallet);
        }
        getPayOrRechargeList(context, handler, payTrdList);
    }

    /**
     * 获取支付方式列表
     */
    private static void getPayOrRechargeList(final Context context, final Handler handler, final List<String> payTrdList) {
        final ResourceManager manager = ResourceManager.getInstance(context);
        final Map<String, String> idMap = getTrdPayDrawableIdMap();


        List<PayTrdItemBean> list = new ArrayList<>();
        int i = 0;
        for (String name : payTrdList) {
            PayTrdItemBean bean = new PayTrdItemBean(name, i++, 100);
            bean.setDrawableId(manager.getIdentifier(idMap.get(bean.get3rdPayName()), "drawable"));
            list.add(bean);
        }

        UCommUtil.sendMesg(handler, PayConst.CODE_PAY_GET_TRD_PAY_LIST, list);
    }

    private static Map<String, String> getTrdPayDrawableIdMap() {
        Map<String, String> idMap = new HashMap<String, String>();
        idMap.put("aliPay", "ema_3rd_btn_alipay");
        idMap.put("weixinPay", "ema_3rd_btn_weixin");
        idMap.put("qqWallet", "ema_3rd_btn_qq");
        //idMap.put("sdopay_card", "ema_3rd_btn_gamecardpay");
        //idMap.put("mobile", "ema_3rd_btn_phonecardpay");
        //idMap.put("wallet", "ema_qianbao");
        //idMap.put("lingyuanfu", "ema_3rd_btn_0yuanfu");
        return idMap;
    }

    /**
     * 使用qq钱包
     *
     * @param activity
     */
    public static void GoRechargeByQQwallet(Activity activity, EmaPayInfo payInfo, Handler handler) {
        boolean isSupportQQpay = (int)USharedPerUtil.getParam(activity, EmaConst.SUPPORT_QQ_PAY, 0) == 1;
        if(!isSupportQQpay){
            ToastHelper.toast(activity,"暂不支持");
            return;
        }
        LOG.d(TAG, "使用qq钱包进行充值");
        mPayInfo=payInfo;
        TrdQQwalletPay.startRecharge(activity, payInfo, handler);
    }

    /**
     * 使用游戏卡进行 [充值]
     */
    public static void GoRechargeByGamecard(Activity activity, EmaPriceBean money) {
        LOG.d(TAG, "使用游戏卡进行充值");
        Intent intent = new Intent(activity, TrdCardActivity.class);
        intent.putExtra(TrdCardActivity.INTENT_AMOUNT, money);
        intent.putExtra(TrdCardActivity.INTENT_TYPE, TrdCardActivity.TYPE_GAMECARD_RECHARGE);
        activity.startActivityForResult(intent, RechargeMabiActivity.INTENT_REQUEST_CODE_GAME_CARD);
    }

    /**
     * 使用手机卡进行 【充值】
     *
     * @param activity
     * @param money
     */
    public static void GoRechargeByPhonecard(Activity activity, EmaPriceBean money) {
        LOG.d(TAG, "使用手机卡进行充值");
        Intent intent = new Intent(activity, TrdCardActivity.class);
        intent.putExtra(TrdCardActivity.INTENT_AMOUNT, money);
        intent.putExtra(TrdCardActivity.INTENT_TYPE, TrdCardActivity.TYPE_PHONECARD_RECHARGE);
        activity.startActivityForResult(intent, RechargeMabiActivity.INTENT_REQUEST_CODE_PHONE_CARD);
    }

    /**
     * 使用支付宝进行
     *
     * @param activity
     */
    public static void GoRecharegeByAlipay(Activity activity, EmaPayInfo payInfo, Handler handler) {
        LOG.d(TAG, "使用支付宝进行充值");
        mPayInfo=payInfo;
        TrdAliPay.startRecharge(activity, payInfo, handler);
    }

    /**
     * 使用微信进行
     *
     * @param activity
     * @param payInfo
     * @param handler
     */
    public static void GoRechargeByWeixin(Activity activity, EmaPayInfo payInfo, Handler handler) {
        boolean isSupportWXpay = (int)USharedPerUtil.getParam(activity, EmaConst.SUPPORT_WX_PAY, 0) == 1;
        if(!isSupportWXpay){
            ToastHelper.toast(activity,"暂不支持");
            return;
        }
        LOG.d(TAG, "使用微信进行充值");
        mPayInfo=payInfo;
        TrdWeixinPay.startRecharge(activity, payInfo,handler);
    }

    /**
     * 使用0元付进行 [充值]
     *
     * @param activity
     * @param money
     * @param handler
     */
    public static void GoRechargeBy0YuanFu(Activity activity, EmaPriceBean money, Handler handler) {
        LOG.d(TAG, "使用0元付进行充值");
        Trd0yuanfuPay.startRecharge(activity, money, handler);
    }

    /**
     * 进入钱包[支付]
     */
    public static void GoPayByMabi(Activity activity, int requestCode) {
        LOG.d(TAG, "进入钱包支付");
        Intent intent = new Intent(activity, PayMabiActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 进入财付通[支付]
     */
    public static void GoPayByTenpay(Activity activity) {
        LOG.d(TAG, "进入财付通支付");
        //TrdQQwalletPay.getPayUrl(activity);
    }

    /**
     * 进入游戏卡[支付]
     *
     * @param activity
     */
    public static void GoPayByGameCardpay(Activity activity, int requetCode) {
        LOG.d(TAG, "进入游戏卡支付");
        Intent intent = new Intent(activity, TrdCardActivity.class);
        //intent.putExtra(TrdCardActivity.INTENT_AMOUNT, EmaPay.getInstance(activity).getPayInfo().getPrice()+"");
        intent.putExtra(TrdCardActivity.INTENT_TYPE, TrdCardActivity.TYPE_GAMECARD_PAY);
        activity.startActivityForResult(intent, requetCode);
    }

    /**
     * 进入手机卡 [支付]
     *
     * @param activity
     */
    public static void GoPayByPhoneCardpay(Activity activity, int requestCode) {
        LOG.d(TAG, "进入手机卡支付");
        Intent intent = new Intent(activity, TrdCardActivity.class);
        //intent.putExtra(TrdCardActivity.INTENT_AMOUNT, EmaPay.getInstance(activity).getPayInfo().getPrice()+"");
        intent.putExtra(TrdCardActivity.INTENT_TYPE, TrdCardActivity.TYPE_PHONECARD_PAY);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 进入支付宝 [支付]
     *
     * @param activity
     */
    public static void GoPayByAlipay(Activity activity, Handler handler) {
        LOG.d(TAG, "进入支付宝支付");
        TrdAliPay.startPay(activity, handler);
    }

    /**
     * 进入微信 【支付】
     *
     * @param activity
     * @param handler
     */
    public static void GoPayByWeixin(Activity activity, Handler handler) {
        LOG.d(TAG, "进入微信支付");
        //EmaPayProcessManager.getInstance().setWeixinActionType(EmaConst.PAY_ACTION_TYPE_PAY);
        //TrdWeixinPay.startPay(activity);
    }

    /**
     * 进入0元付
     *
     * @param activity
     * @param handler
     */
    public static void GoPayBy0yuanfu(Activity activity, PayTrdItemBean bean, Handler handler) {
        LOG.d(TAG, "0元支付");
        Trd0yuanfuPay.startPay(activity, bean, handler);
    }

    /**
     * 发送充值回调消息
     *
     * @param activity
     * @param what
     * @param obj
     */
    public static void sendRechargeMessage(Activity activity, int what, Object obj) {
        if (activity instanceof RechargeMabiActivity) {
            RechargeMabiActivity ac = (RechargeMabiActivity) activity;
            Message msg = new Message();
            msg.what = what;
            msg.obj = obj;
            ac.onRechargeCallBack(msg);
        }
    }

    /**
     * 发送支付回调消息
     */
    public static void sendPayMessage(Activity activity, int what, Object obj) {
        if (activity instanceof PayTrdActivity) {
            PayTrdActivity ac = (PayTrdActivity) activity;
            Message msg = new Message();
            msg.what = what;
            msg.obj = obj;
            ac.onPayCallBack(msg);
        }
    }

    /**
     * 获取支持一张卡多次充值的游戏卡充值渠道
     *
     * @return
     */
    public static List<Integer> getTrdSelectCardAmoutSupport() {
        List<Integer> list = new ArrayList<Integer>();
        list.add(PayConst.PAY_CHARGE_CHANNEL_GAMECARD_SHENGDA);//盛大卡
        return list;
    }

    /**
     * 根据不同的卡类别，获取卡的分类列表
     *
     * @param type
     * @return
     */
    public static List<PayTrdItemBean> getTrdSelectCardType(int type) {
        if (type == TrdCardActivity.TYPE_GAMECARD_PAY || type == TrdCardActivity.TYPE_GAMECARD_RECHARGE) {
            return getTrdSelectGameCardType();
        } else {
            return getTrdSelectPhoneCardType();
        }
    }

    /**
     * 获取手机卡类型列表
     *
     * @return
     */
    public static List<PayTrdItemBean> getTrdSelectPhoneCardType() {
        List<PayTrdItemBean> list = new ArrayList<PayTrdItemBean>();
        list.add(new PayTrdItemBean("移动卡", PayConst.PAY_CHARGE_CHANNEL_PHONECARD_YIDONG));
        list.add(new PayTrdItemBean("联通卡", PayConst.PAY_CHARGE_CHANNEL_PHONECARD_LIANTONG));
        list.add(new PayTrdItemBean("电信卡", PayConst.PAY_CHARGE_CHANNEL_PHONECARD_DIANXIN));
        return list;
    }

    /**
     * 获取游戏卡类型列表
     *
     * @return
     */
    public static List<PayTrdItemBean> getTrdSelectGameCardType() {
        List<PayTrdItemBean> list = new ArrayList<PayTrdItemBean>();
        list.add(new PayTrdItemBean("盛大卡", PayConst.PAY_CHARGE_CHANNEL_GAMECARD_SHENGDA));
        return list;
    }

    /**
     * 获取游戏卡面额列表
     *
     * @return
     */
    public static List<PayTrdItemBean> getTrdSelectCardAmount(EmaPriceBean amountBean) {
        return addPayTrdItemBean(amountBean, new int[]{10, 50, 100, 200, 500, 1000});
    }

    /**
     * 获取卡的面额列表
     *
     * @param bean
     * @return
     */
    public static List<PayTrdItemBean> getTrdSelectCardAmount(PayTrdItemBean bean, EmaPriceBean amountBean) {
        LOG.d(TAG, "getTrdSelectCardAmount__:" + bean.getDrawableId());
        List<PayTrdItemBean> list = null;
        switch (bean.getDrawableId()) {
            case PayConst.PAY_CHARGE_CHANNEL_GAMECARD_SHENGDA://盛大
                list = addPayTrdItemBean(amountBean, new int[]{1, 2, 3, 5, 9, 10, 15, 25, 30, 35, 45, 50, 100, 350, 1000});
                break;
            case PayConst.PAY_CHARGE_CHANNEL_PHONECARD_YIDONG://移动
                list = addPayTrdItemBean(amountBean, new int[]{10, 20, 30, 50, 100, 200, 300, 500});
                break;
            case PayConst.PAY_CHARGE_CHANNEL_PHONECARD_DIANXIN://电信
                list = addPayTrdItemBean(amountBean, new int[]{10, 20, 30, 50, 100, 200, 300, 500});
                break;
            case PayConst.PAY_CHARGE_CHANNEL_PHONECARD_LIANTONG://联通
                list = addPayTrdItemBean(amountBean, new int[]{10, 20, 30, 50, 100, 200, 300, 500});
                break;
            default:
                break;
        }
        return list;
    }

    /**
     * 添加面额值到面额列表
     */
    private static List<PayTrdItemBean> addPayTrdItemBean(EmaPriceBean amountBean, int[] prices) {
        List<PayTrdItemBean> list = new ArrayList<PayTrdItemBean>();
        for (int price : prices) {
            if (price >= amountBean.getPriceYuan()) {
                list.add(new PayTrdItemBean().setAmount(new EmaPriceBean(price, EmaPriceBean.TYPE_YUAN)));
            }
        }
        return list;
    }


    /**
     * 进行订单查询操作，菊花四秒后查询，ok了就支付成功，否则说会略有延迟
     *  //如果是单纯的充值，orderId是八个×，
     *
     * @param handler
     */
    public static void doCheckOrderStatus(final Handler handler) {

        if("xxxxxxxx".equals(mPayInfo.getOrderId())){   // 这是充值的订单
            ToastHelper.toast(Ema.getInstance().getContext(),"充值成功！");
            return;
        }

        Message message1 = Message.obtain();
        message1.what = EmaProgressDialog.CODE_LOADING_START;
        handler.sendMessage(message1);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("OrderStatus","开始查询");
                Map<String, String> params = new HashMap<String, String>();
                params.put("uid",EmaUser.getInstance().getmUid());
                params.put("appId",mAppId);
                params.put("token",EmaUser.getInstance().getToken());
                params.put("orderId",mPayInfo.getOrderId());

                new HttpInvoker().postAsync(Url.checkOrderStatus(), params,
                        new HttpInvoker.OnResponsetListener() {
                            @Override
                            public void OnResponse(String result) {
                                try {
                                    Message message2 = Message.obtain();
                                    message2.what = EmaProgressDialog.CODE_LOADING_END;
                                    handler.sendMessage(message2);

                                    JSONObject json = new JSONObject(result);
                                    Log.e("doCheckOrderStatus",result);
                                    int isSuccessed = json.getInt("data");

                                    if(isSuccessed==5){   //0初始状态   3cancle  4回传游戏服务器失败  5最终成功
                                        UCommUtil.makePayCallBack(EmaCallBackConst.PAYSUCCESS, "支付成功");
                                        handler.sendEmptyMessage(PayTrdActivity.PAY_ACTIVITY_CLOSE);
                                    }else {
                                        //此处已表征为成功，所以不能说是支付失败，而弹窗说可能略有延迟
                                        UCommUtil.makePayCallBack(EmaCallBackConst.PAYSUCCESS, "支付成功");
                                        handler.sendEmptyMessage(EmaConst.PAY_RESULT_DELAYED);
                                    }
                                } catch (Exception e) {
                                    LOG.e("doCheckOrderStatus", "CheckOrder error", e);
                                    //此处已表征为成功，所以不能说是支付失败，而弹窗说可能略有延迟
                                    UCommUtil.makePayCallBack(EmaCallBackConst.PAYSUCCESS, "支付成功");
                                    handler.sendEmptyMessage(EmaConst.PAY_RESULT_DELAYED);
                                }
                            }
                        });
            }
        }, 5000);
    }

}
