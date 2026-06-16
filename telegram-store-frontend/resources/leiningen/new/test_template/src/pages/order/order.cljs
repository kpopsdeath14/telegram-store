(ns {{name}}.pages.order.order
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   [clojure.string :as str]
   ))

(defn- normalize-order-form [order-form]
  (cond
    (map? order-form) order-form
    (and (string? order-form) (not (str/blank? order-form)))
    (try
      (js->clj (js/JSON.parse order-form) :keywordize-keys true)
      (catch :default _ nil))
    :else nil))

(defn- format-order-form-value [value]
  (cond
    (string? value) value
    (number? value) (str value)
    (boolean? value) (if value "да" "нет")
    (sequential? value) (str/join ", " (map str value))
    (map? value) (str/join ", " (map (fn [[k v]] (str (name k) ": " (format-order-form-value v))) value))
    (nil? value) ""
    :else (str value)))

(def ^:private order-form-labels
  {"surname" "Фамилия"
   "first_name" "Имя"
   "patronymic" "Отчество"
   "mail" "Email"
   "email" "Email"
   "phone" "Телефон"
   "address" "Адрес"
   "pickup_address" "Адрес самовывоза"
   "comment" "Комментарий к заказу"
   "delivery_provider_name" "Способ доставки"
   "delivery_type" "Способ доставки"
   "payment_type" "Способ оплаты"
   "delivery_cost" "Стоимость доставки"})

(def ^:private order-form-order
  ["surname"
   "first_name"
   "patronymic"
   "mail"
   "email"
   "phone"
   "address"
   "pickup_address"
   "comment"
   "delivery_provider_name"
   "delivery_type"
   "payment_type"
   "delivery_cost"])

(def ^:private order-form-order-index
  (zipmap order-form-order (range)))

(defn- normalize-order-form-key [k]
  (-> (if (keyword? k) (name k) (str k))
      str/lower-case
      (str/replace "-" "_")))

(defn- order-form-label [k]
  (let [raw (if (keyword? k) (name k) (str k))
        normalized (normalize-order-form-key k)]
    (or (get order-form-labels normalized)
        (-> raw
            (str/replace "_" " ")
            (str/replace "-" " ")))))

(defn- order-form-items
  ([order-form] (order-form-items order-form (fn [_ v] v)))
  ([order-form value-mapper]
   (let [data (normalize-order-form order-form)]
     (->> data
          (map (fn [[k v]]
                 (let [normalized (normalize-order-form-key k)
                       mapped-value (value-mapper normalized v)]
                   {:key normalized
                    :label (order-form-label k)
                    :value (format-order-form-value mapped-value)})))
          (remove (fn [item] (str/blank? (:value item))))
          (sort-by (fn [item]
                     (get order-form-order-index (:key item) 9999)))
          vec))))

(defn- method-name-map [methods name-key rus-key]
  (->> methods
       (keep (fn [method]
               (let [name (get method name-key)
                     rus (or (get method rus-key) name)]
                 (when (and name rus) [name rus]))))
       (into {})))

(defn- map-method-name [value mapping]
  (let [raw (cond
              (keyword? value) (name value)
              :else value)]
    (if (string? raw)
      (get mapping raw raw)
      value)))

(defn format-date-short [date-str]
  (when date-str
    (let [date-part (first (str/split date-str #"[T ]"))]
      (if (re-matches #"\d{4}-\d{2}-\d{2}" date-part)
        (let [[year month day] (str/split date-part #"-")]
          (str day "." month "." (subs year 2 4)))
        date-part))))

(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))

(defn order_page []
  (let [order-data (:order_current @app-state)
        order-items (or (:jsonb_agg order-data) [])
        status-history (or (:order_status_history order-data) [])
        current-status (:order_current_status order-data)
        Timeline antd/Timeline
        Image antd/Image
        accent-color "var(--color-accent)"
        muted-color "#8b8b8b"]
    (let [web-app (.-WebApp js/Telegram)
          platform (when web-app (.-platform web-app))
          is-tg-mobile? (contains? #{"ios" "android"} platform)
          is-mobile-device? (is-mobile?)
          is-mobile (or is-tg-mobile? is-mobile-device?)
          loading? (or (not (:order_loaded? @app-state))
                       (empty? order-data))
          current-status-clj (when current-status
                               (if (map? current-status)
                                 current-status
                                 (js->clj current-status :keywordize-keys true)))
          status-list (->> status-history
                           (map (fn [status]
                                  (if (map? status)
                                    status
                                    (js->clj status :keywordize-keys true)))))
          timeline-statuses (if (seq status-list)
                              status-list
                              (if current-status-clj [current-status-clj] []))
          last-index (dec (count timeline-statuses))]
      [:div {:style {:paddingTop (if is-mobile 111 24)
                     :paddingRight 20
                     :paddingBottom 36
                     :paddingLeft 20
                     :background "#ffffff"}}
       (when loading?
         [:div {:style {:display "flex"
                        :flexDirection "column"
                        :gap 16}}
          [:div {:style {:height 24
                         :width "55%"
                         :borderRadius 4
                         :background "#f0f0f0"}}]
          (for [idx (range 2)]
            ^{:key (str "order-item-skel-" idx)}
            [:div {:style {:border "1px solid rgba(53, 53, 53, 0.15)"
                           :borderRadius 6
                           :padding "16px 18px"
                           :display "flex"
                           :gap 16
                           :alignItems "flex-start"}}
             [:div {:style {:width 90
                            :height 120
                            :borderRadius 6
                            :background "#f0f0f0"}}]
             [:div {:style {:flex 1
                            :display "flex"
                            :flexDirection "column"
                            :gap 8}}
              [:div {:style {:height 16
                             :width "70%"
                             :borderRadius 4
                             :background "#f0f0f0"}}]
              [:div {:style {:height 12
                             :width "45%"
                             :borderRadius 4
                             :background "#f0f0f0"}}]
              [:div {:style {:height 12
                             :width "35%"
                             :borderRadius 4
                             :background "#f0f0f0"}}]
              [:div {:style {:height 12
                             :width "40%"
                             :borderRadius 4
                             :background "#f0f0f0"}}]]
             [:div {:style {:marginLeft "auto"
                            :width 56
                            :height 20
                            :borderRadius 4
                            :background "#f0f0f0"}}]])
          [:div {:style {:display "flex"
                         :flexDirection "column"
                         :alignItems "flex-end"
                         :gap 6}}
           [:div {:style {:height 14
                          :width 140
                          :borderRadius 4
                          :background "#f0f0f0"}}]
           [:div {:style {:height 14
                          :width 120
                          :borderRadius 4
                          :background "#f0f0f0"}}]
           [:div {:style {:height 20
                          :width 160
                          :borderRadius 4
                          :background "#f0f0f0"}}]]
          [:div {:style {:height 16
                         :width "45%"
                         :borderRadius 4
                         :background "#f0f0f0"}}]
          [:div {:style {:display "flex"
                         :flexDirection "column"
                         :gap 10}}
           (for [idx (range 3)]
             ^{:key (str "order-status-skel-" idx)}
             [:div {:style {:display "flex"
                            :alignItems "center"
                            :gap 10}}
              [:div {:style {:width 10
                             :height 10
                             :borderRadius "50%"
                             :background "#f0f0f0"}}]
              [:div {:style {:height 12
                             :width "60%"
                             :borderRadius 4
                             :background "#f0f0f0"}}]])]])

       (when (not loading?)
         [:<>
          [:div {:style {:fontSize 32
                         :fontWeight 400
                         :marginBottom 18
                         :color "#111"}}
           "Заказ №" (:order_external_id order-data)
           ]

          [:div {:style {:display "flex"
                         :flexDirection "column"
                         :gap 16
                         :marginBottom 22}}
           (if (empty? order-items)
             [:div {:style {:fontSize 16
                            :color "#8c8c8c"}}
              "Нет товаров в заказе"]
             (for [item order-items]
               (let [product (if (map? item) item (js->clj item :keywordize-keys true))
                     attrs (or (:product_attributes product) {})
                     images (:images attrs)
                     image-src (cond
                                 (string? images) (first (str/split images #" "))
                                 (sequential? images) (first images)
                                 :else nil)
                     name (or (:product_name attrs) (:product_name product))
                     quantity (:quantity product)
                     summ (:summ item)
                     discount-summ (or (:discount_summ product) (:discount_summ item))
                     unit-summ (or discount-summ summ)
                     final_summ (:final_summ item)]
                 ^{:key (or (:product_id attrs) (:product_id product) (str "item-" (hash product)))}
                 [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                                :borderRadius 6
                                :padding "16px 18px"
                                :display "flex"
                                :gap 16
                                :alignItems "flex-start"}}
                  [:> Image {:src (image_uri_maker image-src)
                             :preview false
                             :width 90
                             :height 120
                             :style {:borderRadius 6
                                     :objectFit "cover"}}]
                  [:div {:style {:flex 1
                                 :minWidth 0
                                 :display "flex"
                                 :flexDirection "column"}}
                   [:div {:style {:fontSize 16
                                  :fontWeight 400
                                  :color "#111"}}
                    name]
                   (when (:color attrs)
                     [:div {:style {:fontSize 11
                                    :color "#9a9a9a"
                                    :lineHeight "13px"
                                    :marginTop 12}}
                      (str "Цвет: " (:color attrs))])
                   (when (:size attrs)
                     [:div {:style {:fontSize 11
                                    :color "#9a9a9a"
                                    :lineHeight "13px"}}
                      (str "Размер: " (:size attrs))])
                   (when (:product_id attrs)
                     [:div {:style {:fontSize 11
                                    :color "#9a9a9a"
                                    :lineHeight "13px"}}
                      (str "Артикул: " (:product_id attrs))])
                   [:div {:style {:fontSize 11
                                  :color "#9a9a9a"
                                  :lineHeight "13px"
                                  :marginTop 12}}
                    (str "Кол-во: " quantity " × " unit-summ "₽")]]
                  [:div {:style {:marginLeft "auto"
                                 :alignSelf "stretch"
                                 :display "flex"
                                 :flexDirection "column"
                                 :justifyContent "flex-end"
                                 :alignItems "flex-end"}}
                   [:div {:style {:fontSize 18
                                  :fontWeight 400
                                  :color accent-color}}
                    (str final_summ "₽")]]])))
                    ]

          [:div {:style {:display "flex"
                         :flexDirection "column"
                         :alignItems "flex-end"
                         :gap 6
                         :width "100%"
                         :marginBottom 22}}
           [:div {:style {:fontSize 16
                          :textAlign "right"
                          :color "#6f6f6f"}}
            (str "Сумма: " (:order_summ order-data) "₽")]
           [:div {:style {:fontSize 16
                          :textAlign "right"
                          :color "#6f6f6f"}}
            (str "Доставка: " (:delivery_cost order-data) "₽")]
           [:div {:style {:fontSize 22
                          :fontWeight 600
                          :color "#111"
                          :textAlign "right"}}
            "Итог: "
            [:span {:style {:color accent-color}}
             (str (:order_summ_with_delivery order-data) "₽")]]
           ]

          (let [delivery-methods (let [methods (:delivery_methods @app-state)]
                                   (cond
                                     (vector? methods) methods
                                     (sequential? methods) (vec methods)
                                     (map? methods) [methods]
                                     :else [])
                                   )
                payment-methods (let [methods (:payment_methods @app-state)]
                                  (cond
                                    (vector? methods) methods
                                    (sequential? methods) (vec methods)
                                    (map? methods) [methods]
                                    :else []))
                delivery-map (method-name-map delivery-methods :delivery_provider_name :delivery_provider_name_rus)
                payment-map (method-name-map payment-methods :payment_provider_name :payment_provider_name_rus)
                form-items (order-form-items (:order_form order-data)
                                             (fn [key value]
                                               (cond
                                                 (#{"delivery_provider_name" "delivery_type"} key)
                                                 (map-method-name value delivery-map)
                                                 (= "payment_type" key)
                                                 (map-method-name value payment-map)
                                                 :else value)))]
            (when (seq form-items)
              [:div {:style {:marginBottom 22}}
               [:div {:style {:fontSize 18
                              :fontWeight 500
                              :color "#111"
                              :marginBottom 10}}
                "Данные заказа"]
               [:div {:style {:display "flex"
                              :flexDirection "column"
                              :gap 6}}
                (for [{:keys [label value]} form-items]
                  ^{:key (str "order-form-" label)}
                  [:div {:style {:display "flex"
                                 :gap 8
                                 :fontSize 14
                                 :color "#6f6f6f"
                                 :width "100%"}}
                   [:div {:style {:minWidth 140
                                  :color "#111"}}
                    (str label ":")]
                   [:div {:style {:flex 1
                                  :marginLeft "auto"
                                  :textAlign "right"}}
                    value]
                   ]
                  )
                ]
               ]
               )
               )

          (let [track-number (:track_number order-data)]
            (when (and track-number (not (str/blank? track-number)))
              [:div {:style {:marginBottom 22
                             :padding 16
                             :border "1px solid rgba(53, 53, 53, 0.15)"
                             :borderRadius 8
                             :background "#f9f9f9"}}
               [:div {:style {:fontSize 16
                              :fontWeight 500
                              :color "#111"
                              :marginBottom 8}}
                "Трек-номер"]
               [:div {:style {:fontSize 18
                              :fontWeight 600
                              :color accent-color
                              :fontFamily "monospace"
                              :letterSpacing "0.5px"}}
                track-number]]))

          [:div {:style {:fontSize 20
                         :fontWeight 400
                         :color "#111"
                         :marginBottom 14}}
           "Статус заказа: "
           [:span {:style {:color accent-color}}
            (or (:status_name_rus current-status-clj) "в пути")]]

          [:div {:style {:marginTop 8}}
           [:> Timeline
            {:mode "left"
             :items (->> timeline-statuses
                         (map-indexed
                          (fn [idx status]
                            (let [is-last (= idx last-index)
                                  date-label (format-date-short (:status_date status))
                                  label-color (if is-last accent-color muted-color)
                                  text-color (if is-last accent-color "#6f6f6f")
                                  dot-node (as-element
                                            (if is-last
                                              [:span {:style {:width 12
                                                              :height 12
                                                              :borderRadius "50%"
                                                              :border (str "1px solid " accent-color)
                                                              :background "#ffffff"
                                                              :display "flex"
                                                              :alignItems "center"
                                                              :justifyContent "center"
                                                              :boxSizing "border-box"}}
                                               [:span {:style {:width 6
                                                               :height 6
                                                               :borderRadius "50%"
                                                               :background accent-color}}]]
                                              [:span {:style {:width 10
                                                              :height 10
                                                              :borderRadius "50%"
                                                              :background accent-color
                                                              :display "block"}}]))]
                              {:color accent-color
                               :dot dot-node
                               :label (as-element
                                       [:span {:style {:fontSize 12
                                                       :color label-color}}
                                        date-label])
                               :children (as-element
                                          [:span {:style {:fontSize 14
                                                          :color text-color}}
                                           (:status_name_rus status)])})))
                         vec)
                         }
                         ]
                         ]
                         ]
                         )
                         ]
                         )
                         )
                         )
