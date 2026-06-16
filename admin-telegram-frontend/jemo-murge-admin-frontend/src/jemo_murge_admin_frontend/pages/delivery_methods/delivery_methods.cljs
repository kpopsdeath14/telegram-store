(ns jemo-murge-admin-frontend.pages.delivery-methods.delivery-methods
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.events.cdek-search-city :refer [cdek_search_city]]
   [jemo-murge-admin-frontend.events.cdek-search-pvz :refer [cdek_search_pvz]]
   [jemo-murge-admin-frontend.events.delivery-provider-add :refer [delivery_provider_add]]
   [jemo-murge-admin-frontend.events.delivery-provider-del :refer [delivery_provider_del]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as string]
   )
  )

(defn method-display-name [method]
  (or (:delivery_provider_name_rus method)
      (some-> (:delivery_provider_name method)
              (string/replace #"-" " ")
              (string/capitalize))
      "Без названия"))

(defn pvz-provider? [provider-name]
  (string/includes? (str provider-name) "pvz"))

(defn pvz-marketplace-name [provider-name]
  (cond
    (string/includes? provider-name "ozon") "Ozon"
    (string/includes? provider-name "wb") "Wildberries"
    (string/includes? provider-name "yandex") "Яндекс Маркета"
    :else ""))

(defn provider-icon [provider-name]
  (let [Icon (cond
               (= provider-name "default") icons/CarOutlined
               (= provider-name "self_taken") icons/ShopOutlined
               (pvz-provider? provider-name) icons/InboxOutlined
               :else icons/EnvironmentOutlined)]
    [:> Icon {:style {:font-size 20
                      :color "#1f1f1f"}}]))

(def cdek-tariff-codes [136 137 138 139])

(def cdek-tariff-info
  {136 {:title "Посылка склад-склад"
        :description "ПВЗ → ПВЗ. Нужен ПВЗ отправления и выбор ПВЗ получателя."
        :from-type :pvz
        :to-type :pvz}
   137 {:title "Посылка склад-дверь"
        :description "ПВЗ → дверь. Нужен ПВЗ отправления и адрес получателя."
        :from-type :pvz
        :to-type :door}
   138 {:title "Посылка дверь-склад"
        :description "Дверь → ПВЗ. Нужен адрес отправления и выбор ПВЗ получателя."
        :from-type :door
        :to-type :pvz}
   139 {:title "Посылка дверь-дверь"
        :description "Дверь → дверь. Нужны адрес отправления и адрес получателя."
        :from-type :door
        :to-type :door}})

(defn bool-value [value]
  (cond
    (boolean? value) value
    (string? value) (contains? #{"true" "1" "yes" "y"} (string/lower-case value))
    :else false))

(defn pvz-point-code [point]
  (or (:code point)
      (get point "code")
      (:point_code point)
      (get point "point_code")))

(defn pvz-point-address [point]
  (or (get-in point [:address :address])
      (get-in point ["address" "address"])
      (:address point)
      (get point "address")
      (:full_address point)
      (get point "full_address")
      (get-in point [:location :address])
      (get-in point ["location" "address"])))

(defn pvz-point-name [point]
  (or (:name point)
      (get point "name")
      (pvz-point-code point)))

(defn pvz-point-label [point]
  (let [code (pvz-point-code point)
        name (pvz-point-name point)
        address (pvz-point-address point)]
    (str (or name "")
         (when (and name address) " — ")
         (or address "")
         (when code (str " (" code ")")))))

(defn safe-string [value]
  (cond
    (string? value) value
    (nil? value) ""
    :else (str value)))

(defn delivery-method-card [method]
  (let [active? (:is_active method)
        provider-name (:delivery_provider_name method)]
    [:div {:style {:background-color "white"
                   :border "1px solid #f0f0f0"
                   :border-radius 18
                   :padding 20
                   :height "100%"
                   :cursor "pointer"
                   :transition "all 0.2s ease"
                   :box-shadow "0 2px 8px rgba(0, 0, 0, 0.08)"}
           :on-mouse-enter (fn [e]
                             (set! (.. e -currentTarget -style -boxShadow)
                                   "0 6px 16px rgba(0, 0, 0, 0.15)"))
           :on-mouse-leave (fn [e]
                             (set! (.. e -currentTarget -style -boxShadow)
                                   "0 2px 8px rgba(0, 0, 0, 0.08)"))
           :on-click (fn []
                       (set! (.-href (.-location js/window))
                             (str "#/delivery-methods/" (js/encodeURIComponent provider-name))))}
     [:div {:style {:display "flex"
                    :justify-content "space-between"
                    :align-items "flex-start"
                    :gap 12}}
      [:div {:style {:display "flex"
                     :align-items "center"
                     :gap 12}}
       [:div {:style {:width 44
                      :height 44
                      :min-width 44
                      :min-height 44
                      :flex-shrink 0
                      :border-radius 14
                      :display "flex"
                      :align-items "center"
                      :justify-content "center"
                      :background-color "#f5f5f5"}}
        [provider-icon provider-name]]
       [:div
        [:div {:style {:font-size 16
                       :font-weight 600}}
         (method-display-name method)]
        [:div {:style {:font-size 12
                       :color "#8c8c8c"
                       :margin-top 4}}
         provider-name]]]
      [:> antd/Tag {:color (if active? "green" "volcano")
                   :style {:border-radius 999
                           :font-size 12
                           :margin 0}}
       (if active? "Подключен" "Не подключен")]]
     [:div {:style {:margin-top 16
                    :display "flex"
                    :align-items "center"
                    :justify-content "space-between"
                    :gap 12}}
      [:div {:style {:font-size 13
                     :color "#595959"}}
       (if (or (#{"default" "self_taken"} provider-name) (pvz-provider? provider-name))
         "Доступно по умолчанию"
         "Открыть настройку и инструкцию")]
      [:> icons/RightOutlined {:style {:color "#bfbfbf"}}]]]))

(defn delivery-method-instructions [method]
  (let [provider-name (:delivery_provider_name method)]
    (cond
      (= provider-name "default")
      ["Этот способ доставки работает по умолчанию."
       "Заказ доставляется курьером или службой доставки."
       "Вы можете оставить его включенным для всех заказов."]
      (= provider-name "self_taken")
      ["Покупатель забирает заказ самостоятельно."
       "Укажите условия получения при необходимости."
       "Вы можете включить или отключить самовывоз."]
      (= provider-name "cdek")
      [[:span "Шаг 1. Заключите договор со СДЭК для интеграции интернет-магазина (это первый шаг для API-интеграции). Подробности: "
        [:a {:href "https://cdekrussia.ru/integration"
             :target "_blank"
             :rel "noopener noreferrer"}
         "cdekrussia.ru/integration"]
        "."]
       [:span "Шаг 2. Войдите в личный кабинет СДЭК: "
        [:a {:href "https://www.cdek.delivery/lk"
             :target "_blank"
             :rel "noopener noreferrer"}
         "www.cdek.delivery/lk"]
        ". Откройте раздел «Интеграция» и нажмите «Создать ключ» — появятся идентификатор аккаунта и пароль."]
       [:span "Шаг 3. Используйте именно идентификатор аккаунта (Account) и пароль (Secure password) из раздела «Интеграция», а не логин/пароль от личного кабинета."]
       [:span "Шаг 4. Скопируйте значения и вставьте их в поля Account и Secure password ниже."]
       [:span "Шаг 5. Настройте тарифы CDEK: включите нужные и укажите адрес отправления или ПВЗ отправления для каждого тарифа. Для тарифов со складом (ПВЗ) получатель выбирает конкретный ПВЗ в каталоге."]]
      (pvz-provider? provider-name)
      (let [marketplace (pvz-marketplace-name provider-name)
            pvz-label (if (empty? marketplace) "ПВЗ" (str "ПВЗ " marketplace))]
        [(str "Доставка в пункт выдачи заказов (" pvz-label ").")
         "Никаких настроек подключения не требуется — просто включите способ доставки."
         (str "При оформлении заказа покупатель самостоятельно вводит адрес ближайшего " pvz-label " в свободной форме.")])
      :else
      ["Создайте аккаунт у провайдера доставки и получите ключ."
       "Вставьте значение в поле ниже — это поможет подключить сервис."
       "Проверьте статус подключения после сохранения."])))

(defn connection-attr [method key]
  (let [string-key (name key)]
    (or (get-in method [:connection_attributes key])
        (get-in method [:connection_attributes string-key])
        "")))

(defn method-provider-token [method]
  (connection-attr method :provider_token))

(defn method-account [method]
  (connection-attr method :account))

(defn method-secure-password [method]
  (connection-attr method :secure_password))

(defn method-cdek-test-mode [method]
  (let [raw (or (connection-attr method :test_mode)
                (connection-attr method :cdek_test_mode)
                (connection-attr method :api_mode)
                (connection-attr method :mode))]
    (bool-value raw)))

(defn method-address [method]
  (connection-attr method :address))

(defn method-default-price [method]
  (connection-attr method :default_price))

(defn method-from-city [method]
  (connection-attr method :from_city))

(defn method-from-city-code [method]
  (connection-attr method :from_city_code))

(defn method-from-pvz-code [method]
  (connection-attr method :from_pvz_code))

(defn method-cdek-tariffs [method]
  (let [attrs (:connection_attributes method)]
    (or (get attrs :tariffs)
        (get attrs "tariffs")
        {})))

(defn cdek-tariffs-configured? [method]
  (let [attrs (:connection_attributes method)]
    (or (and (map? attrs) (contains? attrs :tariffs))
        (and (map? attrs) (contains? attrs "tariffs")))))

(defn cdek-tariff-raw [tariffs code]
  (let [code-str (str code)
        code-kw (keyword code-str)]
    (or (get tariffs code)
        (get tariffs code-str)
        (get tariffs code-kw)
        {})))

(defn cdek-tariff-config [tariffs code default-enabled?]
  (let [cfg (cdek-tariff-raw tariffs code)]
    {:enabled (if (or (contains? cfg :enabled) (contains? cfg "enabled"))
                (bool-value (or (:enabled cfg) (get cfg "enabled")))
                default-enabled?)
     :from_address (or (:from_address cfg) (get cfg "from_address") "")
     :from_pvz_code (or (:from_pvz_code cfg) (get cfg "from_pvz_code") "")}))

(defn normalize-cdek-tariffs [method]
  (let [tariffs (method-cdek-tariffs method)
        has-config (or (seq tariffs) (cdek-tariffs-configured? method))]
    (reduce (fn [acc code]
              (assoc acc code (cdek-tariff-config tariffs code (not has-config))))
            {}
            cdek-tariff-codes)))

(defn build-cdek-tariffs-payload [tariffs]
  (into {}
        (map (fn [[code cfg]]
               [code {:enabled (true? (:enabled cfg))
                      :from_address (:from_address cfg)
                      :from_pvz_code (:from_pvz_code cfg)}])
             (or tariffs {}))))

(defn delivery-method-details [method]
  (let [Input antd/Input
        Button antd/Button
        Card antd/Card
        Switch antd/Switch
        Typography antd/Typography
        Text (.-Text Typography)
        token-value (reagent/atom "")
        account-value (reagent/atom "")
        secure-password-value (reagent/atom "")
        test-mode-value (reagent/atom false)
        address-value (reagent/atom "")
        from-city-value (reagent/atom "")
        from-city-code-value (reagent/atom "")
        from-pvz-code-value (reagent/atom "")
        cdek-city-suggestions (reagent/atom [])
        cdek-city-loading? (reagent/atom false)
        cdek-city-error (reagent/atom nil)
        cdek-pvz-points (reagent/atom [])
        cdek-pvz-loading? (reagent/atom false)
        cdek-pvz-error (reagent/atom nil)
        pvz-search-value (reagent/atom "")
        tariffs-value (reagent/atom {})
        price-value (reagent/atom "")
        last-provider (reagent/atom nil)]
    (fn [method]
      (let [active? (:is_active method)
            provider-name (:delivery_provider_name method)
            default? (= "default" provider-name)
            self-taken? (= "self_taken" provider-name)
            cdek? (= "cdek" provider-name)
            pvz? (pvz-provider? provider-name)
            needs-token? (and (not cdek?) (not (or default? self-taken? pvz?)))
            needs-cdek-credentials? cdek?
            needs-address? self-taken?
            needs-price? (or default? self-taken? pvz?)
            tariffs-valid? (if cdek?
                             (every?
                              (fn [[code cfg]]
                                (let [info (get cdek-tariff-info code)
                                      enabled? (true? (:enabled cfg))
                                      from-type (:from-type info)
                                      tariff-pvz (or (:from_pvz_code cfg) (get cfg "from_pvz_code"))
                                      tariff-address (or (:from_address cfg) (get cfg "from_address"))
                                      global-pvz @from-pvz-code-value]
                                  (if-not enabled?
                                    true
                                    (case from-type
                                      :pvz (not (string/blank? (or tariff-pvz global-pvz "")))
                                      :door (not (string/blank? (or tariff-address "")))
                                      true))))
                              @tariffs-value)
                             true)
            can-toggle? true]
        (when (not= @last-provider provider-name)
          (reset! token-value (method-provider-token method))
          (reset! account-value (method-account method))
          (reset! secure-password-value (method-secure-password method))
          (reset! test-mode-value (method-cdek-test-mode method))
          (reset! address-value (method-address method))
          (reset! from-city-value (method-from-city method))
          (reset! from-city-code-value (method-from-city-code method))
          (reset! from-pvz-code-value (method-from-pvz-code method))
          (reset! cdek-city-suggestions [])
          (reset! cdek-city-loading? false)
          (reset! cdek-city-error nil)
          (reset! cdek-pvz-points [])
          (reset! cdek-pvz-loading? false)
          (reset! cdek-pvz-error nil)
          (reset! pvz-search-value (method-from-pvz-code method))
          (reset! tariffs-value (normalize-cdek-tariffs method))
          (reset! price-value (method-default-price method))
          (when (and cdek? (not (string/blank? (or @from-city-code-value ""))))
            (reset! cdek-pvz-loading? true)
            (cdek_search_pvz
             @from-city-code-value
             ""
             (fn [points]
               (reset! cdek-pvz-points points)
               (reset! cdek-pvz-loading? false)
               (reset! cdek-pvz-error nil))
             (fn [err]
               (reset! cdek-pvz-points [])
               (reset! cdek-pvz-loading? false)
               (reset! cdek-pvz-error err)
               (js/alert (str "CDEK: ошибка загрузки ПВЗ."
                              "\nГород: " (pr-str @from-city-code-value)
                              "\nОшибка: " (pr-str err))))))
          (reset! last-provider provider-name))
        [:div {:style {:padding "24px 30px"}}
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
                      :icon (as-element [:> icons/ArrowLeftOutlined])
                      :onClick (fn []
                                 (set! (.-href (.-location js/window)) "#/delivery-methods"))}
           "К списку"]
          [:div
           [:div {:style {:font-size 22
                          :font-weight 700}}
            (method-display-name method)]
           [:div {:style {:font-size 13
                          :color "#8c8c8c"
                          :margin-top 4}}
            provider-name]]]
         [:div {:style {:display "flex"
                        :align-items "center"
                        :gap 12
                        :margin-bottom 16}}
          [:> antd/Tag {:color (if active? "green" "volcano")
                       :style {:border-radius 999
                               :font-size 12
                               :margin 0}}
           (if active? "Подключен" "Не подключен")]
          (when (= "default" provider-name)
            [:> antd/Tag {:color "blue"
                         :style {:border-radius 999
                                 :font-size 12
                                 :margin 0}}
             "По умолчанию"])
          (when (= "self_taken" provider-name)
            [:> antd/Tag {:color "purple"
                         :style {:border-radius 999
                                 :font-size 12
                                 :margin 0}}
             "Самовывоз"])
          (when pvz?
            [:> antd/Tag {:color "cyan"
                         :style {:border-radius 999
                                 :font-size 12
                                 :margin 0}}
             "ПВЗ"])]

         [:> Card {:title "Инструкция"
                   :style {:border-radius 16
                           :margin-bottom 20}}
          [:div {:style {:display "flex"
                         :flex-direction "column"
                         :gap 10}}
           (doall
            (map-indexed
             (fn [idx item]
               ^{:key (str "instruction-" idx)}
               [:div {:style {:display "flex"
                              :gap 10
                              :align-items "flex-start"}}
                [:div {:style {:min-width 26
                               :height 26
                               :border-radius "50%"
                               :background-color "#f0f5ff"
                               :display "flex"
                               :align-items "center"
                               :justify-content "center"
                               :font-weight 600
                               :color "#2f54eb"}}
                 (inc idx)]
                [:div {:style {:font-size 14
                               :color "#262626"}}
                 item]])
             (delivery-method-instructions method)))]]

         [:> Card {:title "Настройки подключения"
                   :style {:border-radius 16}}
          [:div {:style {:display "flex"
                         :flex-direction "column"
                         :gap 12}}
           (when needs-token?
             [:div
              [:div {:style {:font-size 13
                             :color "#8c8c8c"
                             :margin-bottom 6}}
               "Идентификатор провайдера"]
              [:> Input {:size "large"
                         :addonBefore "provider_token"
                         :value @token-value
                         :placeholder "Введите значение"
                         :onChange (fn [event]
                                     (reset! token-value (.-value (.-target event))))}]])
           (when needs-cdek-credentials?
             [:<>
              [:div
               [:div {:style {:font-size 13
                              :color "#8c8c8c"
                              :margin-bottom 6}}
                "Account (идентификатор аккаунта)"]
               [:> Input {:size "large"
                          :addonBefore "account"
                          :value @account-value
                          :placeholder "Введите Account"
                          :onChange (fn [event]
                                      (reset! account-value (.-value (.-target event))))}]]
              [:div
               [:div {:style {:font-size 13
                              :color "#8c8c8c"
                              :margin-bottom 6}}
                "Secure password (пароль)"]
               [:> Input {:size "large"
                          :addonBefore "secure_password"
                          :value @secure-password-value
                          :placeholder "Введите Secure password"
                          :onChange (fn [event]
                                      (reset! secure-password-value (.-value (.-target event))))}]]])
           (when needs-cdek-credentials?
             [:div {:style {:display "flex"
                            :align-items "center"
                            :gap 12
                            :padding "6px 0"}}
              [:> Switch {:checked @test-mode-value
                          :onChange (fn [checked]
                                      (reset! test-mode-value checked))}]
              [:div {:style {:font-size 13
                             :color "#4c4c4c"}}
               "Тестовый режим (api.edu.cdek.ru)"]])
           (when needs-address?
             [:div
              [:div {:style {:font-size 13
                             :color "#8c8c8c"
                             :margin-bottom 6}}
               "Адрес доставки"]
              [:> Input {:size "large"
                         :addonBefore "address"
                         :value @address-value
                         :placeholder "Введите значение"
                         :onChange (fn [event]
                                     (reset! address-value (.-value (.-target event))))}]])
           (when cdek?
             [:div {:style {:border "1px solid #f0f0f0"
                            :border-radius 12
                            :padding 12
                            :display "flex"
                            :flex-direction "column"
                            :gap 12}}
              [:div {:style {:font-size 14
                             :font-weight 600}}
               "Город и ПВЗ отправления"]
              [:div
               [:div {:style {:font-size 12
                              :color "#8c8c8c"
                              :margin-bottom 6}}
                "Город отправления"]
               [:> Input {:size "large"
                          :addonBefore "from_city"
                          :value (or @from-city-value "")
                          :placeholder "Например: Москва"
                          :onChange (fn [event]
                                      (let [value (.-value (.-target event))]
                                       (reset! from-city-value value)
                                       (reset! from-city-code-value "")
                                       (reset! from-pvz-code-value "")
                                       (reset! cdek-pvz-points [])
                                       (reset! cdek-pvz-loading? false)
                                       (reset! cdek-pvz-error nil)
                                       (reset! cdek-city-error nil)
                                        (reset! cdek-city-loading? true)
                                        (cdek_search_city
                                         value
                                         (fn [cities]
                                           (reset! cdek-city-suggestions cities)
                                           (reset! cdek-city-loading? false)
                                           (reset! cdek-city-error nil))
                                         (fn [err]
                                           (reset! cdek-city-suggestions [])
                                           (reset! cdek-city-loading? false)
                                           (reset! cdek-city-error err)
                                           (js/alert (str "CDEK: ошибка поиска города."
                                                          "\nЗапрос: " (pr-str value)
                                                          "\nОшибка: " (pr-str err)))
                                           )
                                         )
                                        )
                          )
                          }
                          ]
                          ]
              (when @cdek-city-loading?
                [:div {:style {:font-size 12
                               :color "#8c8c8c"}}
                 "Поиск города..."])
              (when @cdek-city-error
                [:div {:style {:font-size 12
                               :color "#d4380d"}}
                 "Ошибка поиска города"])
              (when (seq @cdek-city-suggestions)
                [:div {:style {:display "flex"
                               :flex-direction "column"
                               :gap 8
                               :max-height 200
                               :overflow-y "auto"
                               :padding 4
                               :border "1px solid #e5e5e5"
                               :border-radius 12}}
                 (for [city (take 8 @cdek-city-suggestions)]
                   (let [city-name (:name city)
                         raw-code (:code city)
                         city-code (if (some? raw-code) (str raw-code) "")]
                     ^{:key (str city-code city-name)}
                     [:div {:style {:padding "10px 12px"
                                    :border "1px solid #d9d9d9"
                                    :border-radius 12
                                    :cursor "pointer"
                                    :background "#ffffff"}
                            :onClick (fn []
                                       (reset! from-city-value city-name)
                                       (reset! from-city-code-value city-code)
                                       (reset! from-pvz-code-value "")
                                       (reset! cdek-pvz-points [])
                                       (reset! cdek-city-suggestions [])
                                       (reset! pvz-search-value "")
                                       (reset! cdek-pvz-error nil)
                                       (reset! cdek-pvz-loading? true)
                                       (cdek_search_pvz
                                        city-code
                                        ""
                                        (fn [points]
                                          (reset! cdek-pvz-points points)
                                          (reset! cdek-pvz-loading? false)
                                          (reset! cdek-pvz-error nil))
                                        (fn [err]
                                          (reset! cdek-pvz-points [])
                                          (reset! cdek-pvz-loading? false)
                                          (reset! cdek-pvz-error err)
                                          (js/alert (str "CDEK: ошибка загрузки ПВЗ."
                                                         "\nГород: " (pr-str city-code)
                                                         "\nОшибка: " (pr-str err))))))}
                      [:div {:style {:font-size 13
                                     :font-weight 500}}
                       city-name]
                      (when city-code
                        [:div {:style {:font-size 12
                                       :color "#8c8c8c"}}
                         (str "Код: " city-code)])]))])
              [:div
               [:div {:style {:font-size 12
                              :color "#8c8c8c"
                              :margin-bottom 6}}
                "ПВЗ отправления (поиск по адресу)"
                ]
               [:> Input {:size "large"
                          :addonBefore "from_pvz_code"
                          :value (or @pvz-search-value "")
                          :placeholder "Начните вводить адрес или название"
                          :onChange (fn [event]
                                      (let [value (.-value (.-target event))
                                            city-code (safe-string @from-city-code-value)]
                                        (reset! pvz-search-value value)
                                        (reset! from-pvz-code-value "")
                                        (when (not (string/blank? city-code))
                                          (reset! cdek-pvz-loading? true)
                                          (cdek_search_pvz
                                           city-code
                                           value
                                           (fn [points]
                                             (reset! cdek-pvz-points points)
                                             (reset! cdek-pvz-loading? false)
                                             (reset! cdek-pvz-error nil))
                                           (fn [err]
                                             (reset! cdek-pvz-points [])
                                             (reset! cdek-pvz-loading? false)
                                             (reset! cdek-pvz-error err)
                                             (js/alert (str "CDEK: ошибка поиска ПВЗ."
                                                            "\nГород: " (pr-str @from-city-code-value)
                                                            "\nЗапрос: " (pr-str value)
                                                            "\nОшибка: " (pr-str err))))))))}]
              (when @cdek-pvz-loading?
                [:div {:style {:font-size 12
                               :color "#8c8c8c"}}
                 "Загрузка списка ПВЗ..."]
                )
              (when @cdek-pvz-error
                [:div {:style {:font-size 12
                               :color "#d4380d"}}
                 "Ошибка получения ПВЗ"]
                )
              (let [address-query (-> (safe-string @pvz-search-value) (.toLowerCase))
                    filter-on? (>= (count address-query) 3)
                    filtered-points (if filter-on?
                                      (filter (fn [point]
                                                (let [label (safe-string (pvz-point-label point))
                                                      address (safe-string (pvz-point-address point))
                                                      haystack (-> (str label " " address) (.toLowerCase))]
                                                  (.includes haystack address-query)))
                                              @cdek-pvz-points)
                                      @cdek-pvz-points)]
                (when (seq filtered-points)
                  [:div {:style {:display "flex"
                                 :flex-direction "column"
                                 :gap 8
                                 :max-height 240
                                 :overflow-y "auto"
                                 :padding 4
                                 :border "1px solid #e5e5e5"
                                 :border-radius 12}}
                   (for [point (take 12 filtered-points)]
                     (let [code (pvz-point-code point)
                           label (pvz-point-label point)
                           selected? (= (str code) (str @from-pvz-code-value))]
                       ^{:key (str code)}
                       [:div {:style (merge {:padding "10px 12px"
                                             :border "1px solid #d9d9d9"
                                             :border-radius 12
                                             :cursor "pointer"
                                             :background (if selected?
                                                           "#f0f7ff"
                                                           "#ffffff")}
                                            (when selected?
                                              {:border-color "#1677ff"}))
                              :onClick (fn []
                                         (reset! from-pvz-code-value code)
                                         (reset! pvz-search-value (str code)))}
                        [:div {:style {:font-size 13
                                       :font-weight 500}}
                         label]
                        ]
                       )
                     )
                     ]
                     )
                     )
              ] 
              (when (seq @from-pvz-code-value)
                [:div {:style {:font-size 12
                               :color "#4c4c4c"}}
                 (str "Выбранный ПВЗ: " @from-pvz-code-value)])
              
              [:div {:style {:font-size 14
                             :font-weight 600}}
               "Тарифы CDEK"]
              
              (doall
               (map
                (fn [code]
                  (let [info (get cdek-tariff-info code)
                        cfg (get @tariffs-value code)]
                    ^{:key (str "cdek-tariff-" code)}
                    [:div {:style {:border "1px solid #f5f5f5"
                                   :border-radius 10
                                   :padding 12
                                   :display "flex"
                                   :flex-direction "column"
                                   :gap 10}}
                     [:div {:style {:display "flex"
                                    :justify-content "space-between"
                                    :gap 10
                                    :align-items "flex-start"}}
                     [:div
                      [:div {:style {:font-size 14
                                     :font-weight 600}}
                       (str code " — " (:title info))]
                      [:div {:style {:font-size 12
                                     :color "#8c8c8c"
                                     :margin-top 4}}
                       (:description info)]
                      [:div {:style {:font-size 12
                                     :color "#8c8c8c"
                                     :margin-top 6}}
                       (let [from-type (:from-type info)
                             to-type (:to-type info)
                             from-text (case from-type
                                         :pvz "ПВЗ из блока выше"
                                         :door "адрес из блока выше"
                                         "данные из блока выше")
                             to-text (case to-type
                                       :pvz "Получатель выбирает ПВЗ."
                                       :door "Доставка до двери."
                                       "Доставка по адресу.")]
                         (str "Отправка: " from-text ". " to-text))]
                      ]
                      [:> antd/Switch {:checked (true? (:enabled cfg))
                                       :onChange (fn [checked]
                                                   (swap! tariffs-value assoc-in [code :enabled] checked))}
                       ]
                      ]
                     ]))
                
                cdek-tariff-codes))])
           

           
           (when needs-price?
             [:div
              [:div {:style {:font-size 13
                             :color "#8c8c8c"
                             :margin-bottom 6}}
               "Стоимость доставки по умолчанию"]
              [:> Input {:size "large"
                         :addonBefore "default_price"
                         :value @price-value
                         :placeholder "Введите значение"
                         :onChange (fn [event]
                                     (reset! price-value (.-value (.-target event))))}]])
           [:> Text {:type "secondary"}
            (cond
              needs-cdek-credentials? "Заполните оба поля и сохраните, чтобы завершить подключение."
              (and cdek? (not tariffs-valid?)) "Заполните данные по выбранным тарифам и сохраните."
              needs-token? "Заполните поле и сохраните, чтобы завершить подключение."
              needs-address? "Заполните поле и сохраните, чтобы завершить подключение."
              :else "Для этого способа доставки идентификатор не требуется.")]
           [:div {:style {:display "flex"
                          :gap 12
                          :flex-wrap "wrap"}}
            [:> Button {:type (if active? "default" "primary")
                        :disabled (or (not can-toggle?)
                                      (and (not active?) needs-token? (string/blank? @token-value))
                                      (and (not active?) needs-cdek-credentials?
                                           (or (string/blank? @account-value)
                                               (string/blank? @secure-password-value)))
                                      (and (not active?) needs-address? (string/blank? @address-value))
                                      (and (not active?) cdek? (not tariffs-valid?)))
                        :onClick (fn []
                                   (if active?
                                     (do
                                       (delivery_provider_del provider-name)
                                       (when needs-token?
                                         (reset! token-value ""))
                                       (when needs-cdek-credentials?
                                         (reset! account-value "")
                                         (reset! secure-password-value ""))
                                       (when needs-address?
                                         (reset! address-value "")))
                                     (let [tariffs-payload (when cdek?
                                                             (build-cdek-tariffs-payload @tariffs-value))]
                                       (delivery_provider_add
                                        provider-name
                                        (cond-> {}
                                          (and needs-token? (not (string/blank? @token-value)))
                                          (assoc :provider_token @token-value)
                                          (and needs-cdek-credentials?
                                               (not (string/blank? @account-value))
                                               (not (string/blank? @secure-password-value)))
                                          (assoc :account @account-value
                                                 :secure_password @secure-password-value)
                                          (and needs-address? (not (string/blank? @address-value)))
                                          (assoc :address @address-value)
                                          cdek?
                                          (assoc :tariffs tariffs-payload)
                                          cdek?
                                          (assoc :from_city (or @from-city-value "")
                                                 :from_city_code (or @from-city-code-value "")
                                                 :from_pvz_code (or @from-pvz-code-value ""))
                                          cdek?
                                          (assoc :test_mode (boolean @test-mode-value))
                                          (and needs-price? (not (string/blank? @price-value)))
                                          (assoc :default_price @price-value))))))}
             (if active? "Отключить" "Подключить")]
            (when (and active? (or needs-token? needs-address? needs-price? cdek?))
              [:> Button {:type "primary"
                          :ghost true
                          :disabled (or (and needs-token? (string/blank? @token-value))
                                        (and needs-cdek-credentials?
                                             (or (string/blank? @account-value)
                                                 (string/blank? @secure-password-value)))
                                        (and needs-address? (string/blank? @address-value)))
                          :onClick (fn []
                                     (let [tariffs-payload (when cdek?
                                                             (build-cdek-tariffs-payload @tariffs-value))]
                                       (delivery_provider_add
                                        provider-name
                                        (cond-> {}
                                          (and needs-token? (not (string/blank? @token-value)))
                                          (assoc :provider_token @token-value)
                                          (and needs-cdek-credentials?
                                               (not (string/blank? @account-value))
                                               (not (string/blank? @secure-password-value)))
                                          (assoc :account @account-value
                                                 :secure_password @secure-password-value)
                                          (and needs-address? (not (string/blank? @address-value)))
                                          (assoc :address @address-value)
                                          cdek?
                                          (assoc :tariffs tariffs-payload)
                                          cdek?
                                          (assoc :from_city (or @from-city-value "")
                                                 :from_city_code (or @from-city-code-value "")
                                                 :from_pvz_code (or @from-pvz-code-value ""))
                                          cdek?
                                          (assoc :test_mode (boolean @test-mode-value))
                                          (and needs-price? (not (string/blank? @price-value)))
                                          (assoc :default_price @price-value)))))}
               "Обновить"])]]
          ]]))))

(defn delivery_methods_page []
  (let [Row antd/Row
        Col antd/Col
        delivery-methods (reagent/cursor app-state [:delivery_methods])
        current-method (reagent/cursor app-state [:current_delivery_provider_name])]
    (fn []
      (let [methods (if (sequential? @delivery-methods) @delivery-methods [])
            method-by-name (into {}
                                 (map (fn [method]
                                        [(:delivery_provider_name method) method])
                                      methods))
            selected-method (get method-by-name @current-method)]
        [:div
         (if selected-method
           [delivery-method-details selected-method]
           [:div {:style {:padding "24px 30px"}}
            [:div {:style {:display "flex"
                           :align-items "center"
                           :gap 16
                           :margin-bottom 24}}
             [:> antd/Button {:style {:background "#D3EAFF"
                                      :color "black"
                                      :height 44
                                      :border-radius 14
                                      :font-size 16
                                      :font-weight 400
                                      :box-shadow "0 2px 8px rgba(0, 2, 5, 0.15)"}
                              :icon (as-element [:> icons/ArrowLeftOutlined])
                              :onClick (fn []
                                         (set! (.-href (.-location js/window)) "#/settings"))}
              "Настройки"]
             [:div
              [:div {:style {:font-size 24
                             :font-weight 700}}
               "Способы доставки"]
              [:div {:style {:font-size 13
                             :color "#8c8c8c"
                             :margin-top 4}}
               "Выберите способ доставки, чтобы посмотреть инструкцию и параметры подключения."]]]

            [:> Row {:gutter [16 16]}
             (doall
              (map (fn [method]
                     ^{:key (:delivery_provider_name method)}
                     [:> Col {:xs 24 :sm 12 :lg 8}
                      [delivery-method-card method]])
                   methods)
              )
             ]
            ]
            )
            ]
            )
            )
            )
            )
