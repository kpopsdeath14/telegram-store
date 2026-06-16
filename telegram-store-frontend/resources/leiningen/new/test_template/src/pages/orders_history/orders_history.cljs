(ns {{name}}.pages.orders-history.orders-history
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   [{{name}}.pages.catalog.logo :refer [logo]]
   )
  )

(defn format-date-string [date-str]
  (when date-str
    (let [parts (clojure.string/split date-str #" ")]
      (first parts))))

(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))

(defn orders_history_page []
  (let [Image antd/Image
        orders_history (reagent/cursor app-state [:orders_history])]
    (fn []
      (let [web-app (.-WebApp js/Telegram)
            platform (when web-app (.-platform web-app))
            is-tg-mobile? (contains? #{"ios" "android"} platform)
            is-mobile-device? (is-mobile?)
            is-mobile (or is-tg-mobile? is-mobile-device?)
            loading? (not (:orders_history_loaded? @app-state))
            ]
        [:div {:style {:background "#ffffff"
                       }
               }
         [logo]
         [:div {:style {:paddingTop 24
                        :paddingRight 20
                        :paddingBottom 36
                        :paddingLeft 20}
                }
          [:div {:style {:fontSize 32
                         :fontWeight 400
                         :marginBottom 20
                         :color "#111"}}
           "Ваши заказы"]
          (if loading?
            [:div {:style {:display "flex"
                           :flexDirection "column"
                           :gap 16}}
             (for [idx (range 3)]
               ^{:key (str "orders-skel-" idx)}
               [:div {:style {:border "1px solid rgba(53, 53, 53, 0.15)"
                              :borderRadius 6
                              :padding "16px 18px"
                              :display "flex"
                              :flexDirection "column"
                              :gap 12}}
                [:div {:style {:display "flex"
                               :justifyContent "space-between"
                               :alignItems "center"}}
                 [:div {:style {:height 16
                                :width "45%"
                                :borderRadius 4
                                :background "#f0f0f0"}}]
                 [:div {:style {:height 12
                                :width 90
                                :borderRadius 4
                                :background "#f0f0f0"}}]]
                [:div {:style {:display "flex"
                               :gap 10}}
                 (for [img-idx (range 3)]
                   ^{:key (str "orders-skel-img-" idx "-" img-idx)}
                   [:div {:style {:width 86
                                  :height 86
                                  :borderRadius 8
                                  :background "#f0f0f0"}}])]
                [:div {:style {:height 14
                               :width "60%"
                               :borderRadius 4
                               :background "#f0f0f0"}}]])]
            (if (empty? @orders_history)
              [:div {:style {:fontSize 16
                             :color "#8c8c8c"}}
               "История заказов пуста"]
              [:div {:style {:display "flex"
                             :flexDirection "column"
                             :gap 16}}
               (for [order @orders_history]
                 (let [order-clj (js->clj order :keywordize-keys true)
                       order-items (:jsonb_agg order-clj)
                       item-count (count order-items)
                       total-sum (reduce + (map :final_summ order-items))
                       images (->> order-items
                                   (map #(get-in % [:product_attributes :images]))
                                   (filter some?)
                                   (map (fn [images]
                                          (if (string? images)
                                            (first (clojure.string/split images #" "))
                                            (first images))))
                                   (take 3)
                                   (filter some?))]
                   ^{:key (or (:order_id order-clj) (:order_id order) (str "order-" (hash order-clj)))}
                   [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                                  :borderRadius 6
                                  :padding "16px 18px"
                                  :display "flex"
                                  :flexDirection "column"
                                  :gap 14}
                          :onClick (fn []
                                     (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                     (when-let [order-id (:order_id order-clj)]
                                       (set! (.-href (.-location js/window)) (str "#/order/" order-id)))
                                     (swap! app-state assoc :page :order)
                                     (js/window.scrollTo 0 0)
                                     (swap! app-state assoc :order_current order-clj))}
                    [:div {:style {:display "flex"
                                   :justifyContent "space-between"
                                   :alignItems "center"}}
                     [:div {:style {:fontSize 18
                                    :fontWeight 400
                                    :color "#111"}}
                      (get-in order-clj [:order_current_status :status_name_rus])]
                     [:div {:style {:fontSize 14
                                    :color "#7a7a7a"}}
                      (str "Заказ от " (format-date-string (:order_date order-clj)))]]
                    [:div {:style {:display "flex"
                                   :gap 12}}
                     (for [image images]
                       ^{:key image}
                       [:> Image {:src (image_uri_maker image)
                                  :height 86
                                  :width 86
                                  :preview false
                                  :style {:borderRadius 8
                                          :objectFit "cover"}}])]
                    [:div {:style {:fontSize 14
                                   :color "#6f6f6f"}}
                     (str item-count " "
                          (case item-count
                            1 "товар"
                            (if (and (>= item-count 5) (<= item-count 20)) "товаров" "товара"))
                          " на сумму " total-sum "₽")]
                    (when-let [track-number (:track_number order-clj)]
                      (when-not (clojure.string/blank? track-number)
                        [:div {:style {:fontSize 13
                                       :color "var(--color-accent)"
                                       :marginTop 4
                                       :fontFamily "monospace"
                                       :fontWeight 500}}
                         (str "Трек-номер: " track-number)]))]))]))

          ] 
         ]
         )
       )
     )
   )
