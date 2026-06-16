(ns jemo-murge-admin-frontend.pages.payment-methods.payment-methods
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.events.payment-provider-add :refer [payment_provider_add]]
   [jemo-murge-admin-frontend.events.payment-provider-del :refer [payment_provider_del]]
   [jemo-murge-admin-frontend.events.payment-provider-validate :refer [payment_provider_validate]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as string]
   [jemo-murge-admin-frontend.pages.payment-methods.modal-help :refer [modal_help]]
   )
  )


(def default-payment-method
  {:payment_provider_name "default"
   :payment_provider_name_rus "Оплата при получении"
   :is_active true
   :connection_attributes nil})

(defn ensure-default-method [methods]
  (let [normalized (vec (or methods []))]
    (if (some #(= "default" (:payment_provider_name %)) normalized)
      normalized
      (into [default-payment-method] normalized))))

(defn method-display-name [method]
  (or (:payment_provider_name_rus method)
      (some-> (:payment_provider_name method)
              (string/replace #"-" " ")
              (string/capitalize))
      "Без названия"))

(defn provider-icon [provider-name]
  (let [Icon (case provider-name
               "default" icons/WalletOutlined
               "youkassa" icons/BankOutlined
               "cloudpayments" icons/ThunderboltOutlined
               "tinkoff" icons/CreditCardOutlined
               icons/CreditCardOutlined)]
    [:> Icon {:style {:font-size 20
                      :color "#1f1f1f"}}]))

(defn payment-method-card [method]
  (let [active? (:is_active method)
        provider-name (:payment_provider_name method)]
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
                             (str "#/payment-methods/" (js/encodeURIComponent provider-name))))}
     [:div {:style {:display "flex"
                    :justify-content "space-between"
                    :align-items "flex-start"
                    :gap 12}}
      [:div {:style {:display "flex"
                     :align-items "center"
                     :gap 12}}
       [:div {:style {:width 44
                      :height 44
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
       (if (= "default" provider-name)
         "Можно отключить при необходимости"
         "Открыть настройку и инструкцию")]
      [:> icons/RightOutlined {:style {:color "#bfbfbf"}}]]
     [:> antd/Button {:shape "circle"
                      :style {:margin-top 12}
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn [e]
                                 (.stopPropagation e)
                                 (swap! app-state assoc :payment_methods_help_type provider-name)
                                 (swap! app-state assoc :payment_methods_help_modal_open? true))}]]))

(defn payment-method-instructions [method]
  (let [provider-name (:payment_provider_name method)]
    (cond
      (= "default" provider-name)
      ["Этот способ оплаты можно отключить при необходимости."
       "Покупатель оплачивает заказ при получении."
       "Включайте его только если это актуально для вашего магазина."]

      (= "youkassa" provider-name)
      [[:span "Если ЮKassa ещё не подключена, зарегистрируйтесь: "
        [:a {:href "https://yookassa.ru/connection"
             :target "_blank"
             :rel "noopener noreferrer"}
         "yookassa.ru/connection"]
        ". Создайте магазин и пройдите настройку приема платежей: "
        [:a {:href "https://yookassa.ru/my/boarding/accepting-payments"
             :target "_blank"
             :rel "noopener noreferrer"}
         "yookassa.ru/my/boarding/accepting-payments"]
        "."]
       [:span "Далее подключите бота к ЮKassa через @BotFather: /mybots → ваш бот → Payments → Connect YooKassa. В процессе нужно авторизоваться в ЮKassa и выбрать магазин."]
       [:span "Важно: магазин должен работать на протоколе API. Если в настройках указан другой протокол, обратитесь в поддержку ЮKassa и сообщите ShopID магазина."]
       [:span "После подключения @BotFather выдаст provider_token — именно его нужно вставить в поле provider_token ниже (это не \"секретный ключ ЮKassa\")."]
       [:span "Официальная инструкция: "
        [:a {:href "https://yookassa.ru/docs/support/payments/onboarding/integration/cms-module/telegram"
             :target "_blank"
             :rel "noopener noreferrer"}
         "yookassa.ru/docs/support/payments/onboarding/integration/cms-module/telegram"]
        "."]
       [:span "Возникли вопросы? Напишите сюда: "
        [:a {:href "https://t.me/tr1j3Tz"
             :target "_blank"
             :rel "noopener noreferrer"}
         "@tr1j3Tz"]
        "."]]

      :else
      ["Создайте аккаунт у провайдера и получите идентификатор."
       "Вставьте значение в поле ниже — это поможет связать оплату."
       "Проверьте статус подключения после сохранения."])))

(defn method-provider-token [method]
  (or (get-in method [:connection_attributes :provider_token])
      (get-in method [:connection_attributes "provider_token"])
      ""))

(defn validation-error-text [resp]
  (let [desc (or (:error resp) (:description resp))
        low (some-> desc string/lower-case)]
    (cond
      (string/blank? desc)
      "Не удалось проверить токен. Попробуйте позже."

      (or (string/includes? low "unauthorized")
          (string/includes? low "invalid")
          (string/includes? low "forbidden")
          (string/includes? low "token")
          (string/includes? low "api key"))
      "Неверный токен Юкассы. Проверьте и попробуйте снова."

      (or (string/includes? low "timeout")
          (string/includes? low "timed out")
          (string/includes? low "network"))
      "Не удалось связаться с Юкассой. Попробуйте позже."

      :else
      "Не удалось проверить токен. Проверьте данные и попробуйте снова.")))

(defn payment-method-details [method]
  (let [Input antd/Input
        Button antd/Button
        Card antd/Card
        Typography antd/Typography
        Text (.-Text Typography)
        input-value (reagent/atom "")
        validating? (reagent/atom false)
        last-provider (reagent/atom nil)]
    (fn [method]
      (let [active? (:is_active method)
            provider-name (:payment_provider_name method)
            default? (= "default" provider-name)
            youkassa? (= "youkassa" provider-name)
            message (.-message antd)
            show-error (fn [text]
                         (when message
                           (.error message (str "Проблема с подключением оплаты: " text))
                           )
                         )
            validate-token (fn [on-success]
                             (cond
                               (not youkassa?) (on-success)
                               (string/blank? @input-value)
                               (show-error "Введите токен Юкассы")
                               :else
                               (do
                                 (reset! validating? true)
                                 (payment_provider_validate
                                  provider-name
                                  @input-value
                                  (fn [_]
                                    (reset! validating? false)
                                    (on-success))
                                  (fn [resp]
                                    (reset! validating? false)
                                    (show-error (validation-error-text resp)))))))
            ]
        (when (not= @last-provider provider-name)
          (reset! input-value (method-provider-token method))
          (reset! validating? false)
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
                                 (set! (.-href (.-location js/window)) "#/payment-methods"))}
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
             "По умолчанию"])]

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
                 (inc idx)
                 ]
                [:div {:style {:font-size 14
                               :color "#262626"}}
                 item]])
             (payment-method-instructions method)))]]

         [:> Card {:title "Настройки подключения"
                   :style {:border-radius 16}}
          [:div {:style {:display "flex"
                         :flex-direction "column"
                         :gap 12}}
           [:div
            [:div {:style {:font-size 13
                           :color "#8c8c8c"
                           :margin-bottom 6}}
             "Идентификатор провайдера"]
            [:> Input {:size "large"
                       :addonBefore "provider_token"
                       :value @input-value
                       :placeholder "Введите значение"
                       :onChange (fn [event]
                                   (reset! input-value (.-value (.-target event))))
                       :disabled default?}]]
           [:> Text {:type "secondary"}
            (if (= "default" provider-name)
              "Для метода по умолчанию идентификатор не требуется."
              "Заполните поле и сохраните, чтобы завершить подключение.")]
           [:div {:style {:display "flex"
                          :gap 12
                          :flex-wrap "wrap"}}
            [:> Button {:type (if active? "default" "primary")
                        :loading (and youkassa? @validating?)
                        :disabled (or @validating?
                                      (and (not active?) (not default?) (= @input-value "")))
                        :onClick (fn []
                                   (if active?
                                     (do
                                       (payment_provider_del provider-name)
                                       (reset! input-value "")
                                       )
                                     (validate-token
                                      (fn []
                                        (payment_provider_add provider-name {:provider_token @input-value})
                                        )
                                      )
                                     )
                                   )
                        }
             (if active? "Отключить" "Подключить")]
            (if active? 
              [:> Button {:type "primary"
                          :ghost true
                          :loading (and youkassa? @validating?)
                          :disabled (or @validating? (= @input-value "") default?)
                          :onClick (fn []
                                     (validate-token
                                      (fn []
                                        (payment_provider_add provider-name {:provider_token @input-value})))
                                     )
                          }
               "Обновить"]
              )
            ]
           ]
           ]
           ]
           )
           )
           )
           )

(defn payment_methods_page []
  (let [Row antd/Row
        Col antd/Col
        payment-methods (reagent/cursor app-state [:payment_methods])
        current-method (reagent/cursor app-state [:current_payment_provider_name])]
    (fn []
      (let [methods (ensure-default-method
                     (if (sequential? @payment-methods) @payment-methods []))
            method-by-name (into {}
                                 (map (fn [method]
                                        [(:payment_provider_name method) method])
                                      methods))
            selected-method (get method-by-name @current-method)]
        [:div
         (if selected-method
           [payment-method-details selected-method]
           [:div {:style {:padding "24px 30px"}}
            [modal_help]
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
              [:div {:style {:display "flex" :align-items "center" :gap 8}}
               [:div {:style {:font-size 24
                              :font-weight 700}}
                "Способы оплаты"]
               [:> antd/Button {:shape "circle"
                                :icon (as-element [:> icons/QuestionCircleOutlined])
                                :onClick (fn []
                                           (swap! app-state assoc :payment_methods_help_type "page")
                                           (swap! app-state assoc :payment_methods_help_modal_open? true))}]]
              [:div {:style {:font-size 13
                             :color "#8c8c8c"
                             :margin-top 4}}
               "Выберите метод, чтобы посмотреть инструкцию и параметры подключения."]]]

            [:> Row {:gutter [16 16]}
             (doall
              (map (fn [method]
                     ^{:key (:payment_provider_name method)}
                     [:> Col {:xs 24 :sm 12 :lg 8}
                      [payment-method-card method]
                      ]
                     )
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
           
