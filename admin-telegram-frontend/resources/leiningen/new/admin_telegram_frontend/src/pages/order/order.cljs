(ns {{name}}.pages.order.order
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.api-uri-maker :refer [image_uri_maker]]
   [{{name}}.events.order-status-set :refer [order_status_set]]
   [{{name}}.events.order-track-number-set :refer [order_track_number_set]]
   [clojure.string :as str]
   [{{name}}.pages.order.modal-help :refer [modal_help]]
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
   "pvz_address" "Адрес ПВЗ"
   "comment" "Комментарий к заказу"
   "delivery_provider_name" "Способ доставки"
   "delivery_type" "Способ доставки"
   "payment_type" "Способ оплаты"
   "delivery_cost" "Стоимость доставки"
   "cdek_city" "Город СДЭК"
   "cdek_city_code" "Код города СДЭК"
   "cdek_pvz_address" "Адрес ПВЗ СДЭК"
   "cdek_pvz_code" "Код ПВЗ СДЭК"
   "cdek_tariff_code" "Тариф СДЭК"})

(def ^:private order-form-hidden-keys
  #{"cdek_pvz"})

(def ^:private order-form-order
  ["surname"
   "first_name"
   "patronymic"
   "mail"
   "email"
   "phone"
   "address"
   "pickup_address"
   "pvz_address"
   "comment"
   "delivery_provider_name"
   "delivery_type"
   "payment_type"
   "delivery_cost"
   "cdek_city"
   "cdek_city_code"
   "cdek_pvz_address"
   "cdek_pvz_code"
   "cdek_tariff_code"])

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

(defn- order-form-items [order-form delivery-name-map payment-name-map]
  (let [data (normalize-order-form order-form)]
    (->> data
         (map (fn [[k v]]
                (let [normalized (normalize-order-form-key k)
                      raw-value  (format-order-form-value v)
                      display-value (cond
                                      (contains? #{"delivery_type" "delivery_provider_name"} normalized)
                                      (or (get delivery-name-map raw-value) raw-value)
                                      (= "payment_type" normalized)
                                      (or (get payment-name-map raw-value) raw-value)
                                      :else raw-value)]
                  {:key normalized
                   :label (order-form-label k)
                   :value display-value})))
         (remove (fn [item] (contains? order-form-hidden-keys (:key item))))
         (remove (fn [item] (str/blank? (:value item))))
         (sort-by (fn [item]
                    (get order-form-order-index (:key item) 9999)))
         vec)))





(defn order_page []
  (let [track-number-value (reagent/atom "")
        last-order-id (reagent/atom nil)]
    (fn []
      (let [order-data (:order @app-state)
            order-items (:jsonb_agg order-data)
            status-history (:order_status_history order-data)
            current-status (:order_current_status order-data)
            statuses (reagent/cursor app-state [:statuses])
            order-id (:order_id order-data)
            existing-track (:track_number order-data)
            delivery-methods (let [m (:delivery_methods @app-state)]
                               (cond (sequential? m) m (map? m) [m] :else []))
            payment-methods  (let [m (:payment_methods @app-state)]
                               (cond (sequential? m) m (map? m) [m] :else []))
            delivery-name-map (into {} (map (fn [m] [(:delivery_provider_name m)
                                                      (:delivery_provider_name_rus m)])
                                            delivery-methods))
            payment-name-map  (into {} (map (fn [m] [(:payment_provider_name m)
                                                      (:payment_provider_name_rus m)])
                                            payment-methods))

            Timeline antd/Timeline
            Card antd/Card
            List antd/List
            Select antd/Select
            Image antd/Image
            Button antd/Button
            Input antd/Input
            ListItem (.-Item List)]

        (when (not= order-id @last-order-id)
          (reset! last-order-id order-id)
          (reset! track-number-value (or existing-track "")))

        [:div {:style {:padding "20px"}}
         [modal_help]
         [:div {:style {:display "flex"
                        :align-items "center"
                        :gap 16
                        :margin-bottom 24}}
          [:> Button {:style {:background "#D3EAFF"
                              :color "black"
                              :height 44
                              :border-radius 14
                              :font-size 16
                              :font-weight 400
                              :box-shadow "0 2px 8px rgba(0, 2, 5, 0.15)"}
                      :icon (reagent/as-element [:> icons/ArrowLeftOutlined])
                      :onClick (fn []
                                 (set! (.-href (.-location js/window)) "#/orders-history"))}
           "К заказам"]
          [:div {:style {:font-size 13
                         :color "#8c8c8c"}}
           "История заказов"]]

         ;; Заголовок с номером заказа
         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:> antd/Typography.Title {:level 2}
           "Заказ #" (:order_external_id order-data)]
          [:> Button {:shape "circle"
                      :icon (reagent/as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :order_help_type "order_id")
                                 (swap! app-state assoc :order_help_modal_open? true))}]]

         ;; Дата заказа
         [:> antd/Typography.Text {:type "secondary" :style {:display "block" :margin-bottom "10px"}}
          "Дата оформления: " (:order_date order-data)]

         ;; Трек-номер
         [:div {:style {:margin-bottom "20px"}}
          [:div {:style {:display "flex" :align-items "center" :gap 6 :margin-bottom "8px"}}
           [:> antd/Typography.Text {:strong true} "Трек-номер"]
           [:> antd/Button {:type "text"
                            :size "small"
                            :icon (reagent/as-element [:> icons/QuestionCircleOutlined])
                            :onClick (fn []
                                       (swap! app-state assoc :order_help_type "track_number")
                                       (swap! app-state assoc :order_help_modal_open? true))}]]
          (when (:track_number order-data)
            [:> antd/Typography.Text {:type "secondary" :style {:display "block" :margin-bottom "8px"}}
             "Текущий: " (:track_number order-data)])
          [:div {:style {:display "flex" :gap 12 :flex-wrap "wrap"}}
           [:> Input {:style {:min-width 260}
                      :placeholder "Введите трек-номер"
                      :value @track-number-value
                      :onChange (fn [event]
                                  (reset! track-number-value (.-value (.-target event))))}]
           [:> Button {:type "primary"
                       :disabled (str/blank? @track-number-value)
                       :onClick (fn []
                                  (order_track_number_set (:current_order_id @app-state) @track-number-value))}
            "Сохранить трек-номер"]]]
         
         (when (:user_data order-data)
           (let [tg-link (if-let [username (:username (:user_data order-data))]
                           (str "https://t.me/" username)
                           (str "tg://user?id=" (:id (:user_data order-data))))
                 ]
             [:div {:style {:display "flex" :align-items "center" :gap "8px" :margin-bottom "20px"}}
              [:> antd/Typography.Text {:strong true}
               "ID клиента: " (:telegram_user_id order-data)]
              [:> antd/Button
               {:type "primary"
                :size "middle"
                :href tg-link
                :target "_blank"
                :style {:border-radius 12
                        :box-shadow "0 2px 8px rgba(24, 144, 255, 0.35)"
                        :font-weight 500}
                :icon (reagent/as-element [:i {:class "fas fa-paper-plane"}])}
               "Написать клиенту"]
              [:> Button {:shape "circle"
                          :icon (reagent/as-element [:> icons/QuestionCircleOutlined])
                          :onClick (fn []
                                     (swap! app-state assoc :order_help_type "write_client")
                                     (swap! app-state assoc :order_help_modal_open? true))}]
              ]
             
             )
           )
         

         ;; Правая колонка - статус заказа
         [:div {:style {:flex "1" :min-width "250px"}}
          ;; Текущий статус
          [:> Card {:title "Текущий статус" :size "small" :style {:margin-bottom "20px"}}
           [:div {:style {:text-align "center"}}
            [:div {:style {:display "flex" :align-items "center" :gap 8}}
             [:> Select {:style {:width "100%"}
                         :value (when current-status (:status_name current-status))
                         :options (->> @statuses
                                       (sort-by :status_order_number)
                                       (map (fn [item]
                                              {:label (:status_name_rus item)
                                               :value (:status_name item)})))
                         :onChange #(order_status_set (:current_order_id @app-state) %)}]
             [:> Button {:shape "circle"
                         :icon (reagent/as-element [:> icons/QuestionCircleOutlined])
                         :onClick (fn []
                                    (swap! app-state assoc :order_help_type "status")
                                    (swap! app-state assoc :order_help_modal_open? true))}]]]]
        

          ;; История статусов
          [:> Card {:title "История статусов" :size "small"}
           [:> Timeline
            {:items (->> status-history
                         (map (fn [status]
                                {:color (-> status :status_info :color)
                                 :label (-> status :status_date
                                            (clojure.string/split #"T")
                                            first)
                                 :children (:status_name_rus status)}))
                         reverse)
             :mode "left"}]]]

         [:> Card {:title "Информация о заказе" :size "small" :style {:margin-top "20px"}}
          [:> List
           {:size "small"
            :dataSource [{:label "ID заказа" :value (:order_external_id order-data)}
                         ]
            :renderItem (fn [item]
                          (let [item-data (js->clj item :keywordize-keys true)]
                            (as-element
                             [:> ListItem
                              [:> antd/Typography.Text {:strong true} (:label item-data) ": "]
                              [:> antd/Typography.Text {:type "secondary"} (:value item-data)]])))}]]

         (let [form-items (order-form-items (:order_form order-data) delivery-name-map payment-name-map)]
           (when (seq form-items)
             [:> Card {:title "Данные покупателя" :size "small" :style {:margin-top "20px"}}
              [:> List
               {:size "small"
                :dataSource form-items
                :renderItem (fn [item]
                              (let [item-data (js->clj item :keywordize-keys true)]
                                (as-element
                                 [:> ListItem
                                  [:> antd/Typography.Text {:strong true} (:label item-data) ": "]
                                  [:> antd/Typography.Text {:type "secondary"} (:value item-data)]])))}]]))
         

         [:div {:style {:display "flex"
                        :margin-top "20px"
                        :flex-wrap "wrap"}}
         
          [:div {:style {:flex "2" :min-width "300px"}}
           [:> List
            {:dataSource (if (nil? order-items)
                           []
                           order-items)
             :renderItem (fn [item]
                           (let [product (js->clj item :keywordize-keys true)
                                 attrs (:product_attributes product)
                                 unit-summ (or (:discount_summ product) (:summ product))]
                             (as-element
                              [:> ListItem
                               {:style {:padding "16px 0"}}
                               [:div {:style {:display "flex" :gap "12px" :width "100%"}}
                                [:> Image
                                 {:width 80
                                  :height 80
                                  :style {:object-fit "cover" :border-radius "8px"}
                                  :src (image_uri_maker (-> product :product_attributes :images first))
                                  :preview false}]
         
                                ;; Информация о товаре
                                [:div {:style {:flex "1"}}
                                 [:> antd/Typography.Text {:strong true}
                                  (:product_name attrs)]
                                 [:br]
                                 [:> antd/Typography.Text {:type "secondary" :style {:font-size "12px"}}
                                  (str "Артикул: " (:product_id attrs))]
                                 [:br]
                                 (when (:color attrs)
                                   [:> antd/Typography.Text {:type "secondary" :style {:font-size "12px"}}
                                    (str "Цвет: " (:color attrs))])
                                 [:br]
                                 (when (:size attrs)
                                   [:> antd/Typography.Text {:type "secondary" :style {:font-size "12px"}}
                                    (str "Размер: " (:size attrs))])
                                 [:br]
                                 [:> antd/Typography.Text {:type "secondary" :style {:font-size "12px"}}
                                  (str "Кол-во: " (:quantity product) " × " unit-summ " ₽")]]]
         
                               ;; Итоговая стоимость позиции на отдельной строке
                               [:div {:style {:display "flex" :justify-content "flex-end" :width "100%" :margin-top "8px"}}
                                [:> antd/Typography.Text {:strong true :style {:font-size "16px"}}
                                 (str "Сумма: " (:final_summ product) " ₽")]]])))}]]
         
          ;; Общая сумма заказа
          (let [total (reduce + (map :final_summ order-items))]
            [:div {:style {:text-align "right" :border-top "1px solid #f0f0f0" :width "100%"}}
             [:> antd/Typography.Title {:level 3}
              (str "Итого: " total " ₽")]])]
         
         
         ]
         ))))
