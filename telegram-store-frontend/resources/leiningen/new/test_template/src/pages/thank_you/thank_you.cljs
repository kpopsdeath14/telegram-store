(ns {{name}}.pages.thank-you.thank-you
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]))

(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))

(defn navigate! [href]
  (set! (.-href (.-location js/window)) href)
  (js/window.scrollTo 0 0)
  )

(defn thank_you_page []
  (let [Result antd/Result
        Button antd/Button
        order-id (or (:last_paid_order_id @app-state)
                     (:current_order_id @app-state))
        order-external-id (or (:order_external_id (:order_current @app-state))
                              order-id)
        web-app (.-WebApp js/Telegram)
        platform (when web-app (.-platform web-app))
        is-tg-mobile? (contains? #{"ios" "android"} platform)
        is-mobile-device? (is-mobile?)
        is-mobile (or is-tg-mobile? is-mobile-device?)
        ]
    [:div {:style {:min-height "100vh"
                   :background "#ffffff"
                   :padding-top (if is-mobile 118 56)
                   :padding-right 20
                   :padding-bottom 56
                   :padding-left 20
                   :box-sizing "border-box"
                   :display "flex"
                   :flex-direction "column"}
           }
     [:> Result
      {:status "success"
       :title "Спасибо за покупку"
       :subTitle (str "Заказ №" order-external-id " успешно оформлен.")
       :style {:max-width 460
               :margin "0 auto"
               :padding "8px 0 0"}}
      [:div {:style {:display "flex"
                     :flex-direction "column"
                     :gap 12
                     :width "100%"
                     :max-width 420
                     :margin "20px auto 0"}
             }
       [:> Button
        {:type "primary"
         :size "large"
         :style {:height 48
                 :border-radius 12
                 :font-size 16
                 :font-weight 500
                 :background "#111111"
                 :border-color "#111111"}
         :on-click (fn []
                     (when order-id
                       (swap! app-state assoc :current_order_id order-id)
                       (navigate! (str "#/order/" order-id))))}
        "Открыть заказ"]
       [:> Button
        {:size "large"
         :style {:height 48
                 :border-radius 12
                 :font-size 16
                 :font-weight 500}
         :on-click (fn []
                     (swap! app-state assoc :page :catalog)
                     (navigate! "#/catalog"))}
        "В каталог"]
       ]
       ]
       ]
       )
       )
