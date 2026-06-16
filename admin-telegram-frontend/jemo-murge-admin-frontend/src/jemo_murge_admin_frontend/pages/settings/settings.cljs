(ns jemo-murge-admin-frontend.pages.settings.settings
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker banner_uri_maker]]
   [jemo-murge-admin-frontend.pages.settings.modal-banner-edit :refer [modal_banner_edit]]
   [jemo-murge-admin-frontend.pages.settings.banner-list :refer [banner_list]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.events.settings-set :refer [settings_set]]
   [jemo-murge-admin-frontend.events.catalog-bot-description-set :refer [catalog_bot_description_set]]
   [jemo-murge-admin-frontend.pages.settings.payment-methods :refer [payment_methods]]
   [jemo-murge-admin-frontend.pages.settings.delivery-methods :refer [delivery_methods]]
   [jemo-murge-admin-frontend.pages.settings.managers :refer [managers]]
   [jemo-murge-admin-frontend.pages.settings.modal-help :refer [modal_help]]
   )
)


(defn settings_page []
  (let [store-name (reagent/atom nil)
        contact-nickname (reagent/atom nil)
        start-message (reagent/atom nil)
        catalog-bot-description (reagent/atom nil)]
    (fn []
      (let [Spin antd/Spin
            Input antd/Input
            TextArea (.-TextArea Input)
            Button antd/Button
            Row antd/Row
            Col antd/Col
            settings (reagent/cursor app-state [:settings])
            settings_value @settings
            settings-ready? (seq settings_value)
            description-default (or (get-in settings_value [:telegram_bot :catalog_bot_description])
                                    (get-in settings_value [:telegram_bot :description])
                                    (:catalog_bot_description settings_value)
                                    "")
            start-message-default (or (:start_message settings_value)
                                      (get-in settings_value [:telegram_bot :start_message])
                                      (get-in settings_value [:telegram_bot :welcome_message])
                                      "")

            development (reagent/cursor app-state [:development])
            ]
        [:div
         [modal_help]
         [modal_banner_edit]
       [:> Row {:gutter 16
                :style {:margin-bottom 25}}
        [:> Col {:span 2
                 :style {:display "flex"
                         :flex-direction "column"
                         :height "100%"}}
         [:> Button {:style {:background "#D3EAFF"
                             :color "black"
                             :height 50
                             :border-radius 15
                             :width "100%"
                             :font-size 24
                             :font-weight 300
                             :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"
                             :margin-top "auto"}
                     :onClick (fn []
                                (set! (.-href (.-location js/window)) "#/units"))
                     :icon (as-element [:> icons/ArrowLeftOutlined])}]]]
       [:> Row {:gutter [16 16]
                :style {:margin-bottom 24}}
        [:> Col {:xs 24 :sm 12 :lg 8}
         [:div {:style {:position "relative"}}
          [banner_list]
          [:> Button {:shape "circle"
                      :style {:position "absolute" :top 10 :right 10}
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :settings_help_type "banners")
                                 (swap! app-state assoc :settings_help_modal_open? true))}]]]
        [:> Col {:xs 24 :sm 12 :lg 8}
         [:div {:style {:position "relative"}}
          [payment_methods]
          [:> Button {:shape "circle"
                      :style {:position "absolute" :top 10 :right 10}
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :settings_help_type "payment")
                                 (swap! app-state assoc :settings_help_modal_open? true))}]]]
        [:> Col {:xs 24 :sm 12 :lg 8}
         [:div {:style {:position "relative"}}
          [delivery_methods]
          [:> Button {:shape "circle"
                      :style {:position "absolute" :top 10 :right 10}
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :settings_help_type "delivery")
                                 (swap! app-state assoc :settings_help_modal_open? true))}]]]
        (when-not (= "manager" (:user_status @development))
          [:> Col {:xs 24 :sm 12 :lg 8}
           [:div {:style {:position "relative"}}
            [managers]
            [:> Button {:shape "circle"
                        :style {:position "absolute" :top 10 :right 10}
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :settings_help_type "managers")
                                   (swap! app-state assoc :settings_help_modal_open? true))}]]]
          )
        ]
  
       (if settings-ready?
         [:div {:style {:margin-bottom 20}}
          [:div {:style {:margin-bottom "16px"}}
           [:div {:style {:display "flex" :align-items "center" :gap 8
                          :margin-bottom "8px"}}
            [:div {:style {:font-weight "500"}}
             "Название магазина"]
            [:> Button {:shape "circle"
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :settings_help_type "store_name")
                                   (swap! app-state assoc :settings_help_modal_open? true))}]]
           [:> Input
            {:placeholder "Введите название магазина"
             :size "large"
             :defaultValue (or (get-in settings_value [:telegram_bot :store_name]) "")
             :onChange (fn [event]
                         (let [value (.-value (.-target event))]
                           (reset! store-name value)))
             :style {:width "100%"}}]]
  
          [:div {:style {:margin-top "24px"}}
           [:> Button
            {:type "primary"
             :size "large"
             :style {:width "100%"}
             :onClick (fn []
                        (let [value (if (some? @store-name)
                                      @store-name
                                      (get-in settings_value [:telegram_bot :store_name]))]
                          (settings_set "store_name" value)
                          (reset! store-name nil)))}
            "Сохранить название"]]
  
          [:div {:style {:margin-bottom "16px"
                         :margin-top "20px"}}
           [:div {:style {:display "flex" :align-items "center" :gap 8
                          :margin-bottom "8px"}}
            [:div {:style {:font-weight "500"}}
             "Контактный аккаунт"]
            [:> Button {:shape "circle"
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :settings_help_type "contact")
                                   (swap! app-state assoc :settings_help_modal_open? true))}]]
           [:> Input
            {:placeholder "Введите контактный аккаунт"
             :size "large"
             :addonBefore "@"
             :defaultValue (or (get-in settings_value [:customer :customer_contact_nickname]) "")
             :onChange (fn [event]
                         (let [value (.-value (.-target event))]
                           (reset! contact-nickname value)))
             :style {:width "100%"}}]]
  
          [:div {:style {:margin-top "24px"}}
           [:> Button
            {:type "primary"
             :size "large"
             :style {:width "100%"}
             :onClick (fn []
                        (let [nickname (if (some? @contact-nickname)
                                         @contact-nickname
                                         (get-in settings_value [:customer :customer_contact_nickname]))]
                          (settings_set "customer_contact_nickname" nickname)
                          (reset! contact-nickname nil)))}
            "Сохранить контакт"]]

          [:div {:style {:margin-bottom "16px"
                         :margin-top "20px"}}
           [:div {:style {:display "flex" :align-items "center" :gap 8
                          :margin-bottom "8px"}}
            [:div {:style {:font-weight "500"}}
             "Описание бота каталога (только текст)"]
            [:> Button {:shape "circle"
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :settings_help_type "description")
                                   (swap! app-state assoc :settings_help_modal_open? true))}]]
           [:> TextArea
            {:placeholder "Введите описание для бота каталога"
             :size "large"
             :autoSize {:minRows 3}
             :value (or @catalog-bot-description description-default)
             :onChange (fn [event]
                         (let [value (.-value (.-target event))]
                           (reset! catalog-bot-description value)))
             :style {:width "100%"}}]
           [:div {:style {:margin-top "12px"
                          :margin-bottom "8px"
                          :font-size 12
                          :color "#6B7280"}}
            "Это описание видно в Telegram до нажатия Start."]]

          [:div {:style {:margin-top "16px"}}
           [:> Button
            {:type "primary"
             :size "large"
             :style {:width "100%"}
             :onClick (fn []
                        (let [description (or @catalog-bot-description description-default "")]
                          (catalog_bot_description_set description)))}
            "Сохранить описание бота"]]

          [:div {:style {:margin-bottom "16px"
                         :margin-top "20px"}}
           [:div {:style {:display "flex" :align-items "center" :gap 8
                          :margin-bottom "8px"}}
            [:div {:style {:font-weight "500"}}
             "Приветственное сообщение"]
            [:> Button {:shape "circle"
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :settings_help_type "welcome")
                                   (swap! app-state assoc :settings_help_modal_open? true))}]]
           [:> TextArea
            {:placeholder "Введите приветственное сообщение для пользователей"
             :size "large"
             :autoSize {:minRows 3}
             :defaultValue (or (get-in settings_value [:telegram_bot :start_message]) "")
             :onChange (fn [event]
                         (let [value (.-value (.-target event))]
                           (reset! start-message value)
                           )
                         )
             :style {:width "100%"}}]
           [:div {:style {:margin-top "8px"
                          :font-size 12
                          :color "#6B7280"}}
            "Если оставить пустым — при /start сообщение не отправляется."]]

          [:div {:style {:margin-top "16px"}}
           [:> Button
            {:type "primary"
             :size "large"
             :style {:width "100%"}
             :onClick (fn []
                        (let [value (or @start-message start-message-default "")]
                          (settings_set "start_message" value)
                          (reset! start-message nil)))}
            "Сохранить приветствие"]
           ]
          ]
         [:> Spin {:size "large"} "Загрузка..."])
       ]
      )
    )
  )
  )
