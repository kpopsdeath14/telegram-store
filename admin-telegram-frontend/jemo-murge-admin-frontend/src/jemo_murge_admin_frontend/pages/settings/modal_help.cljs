(ns jemo-murge-admin-frontend.pages.settings.modal-help
  (:require
   ["antd" :as antd]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:settings_help_modal_open?])
        help-type (reagent/cursor app-state [:settings_help_type])
        help-content {"banners"     {:title "Баннеры на главной странице"
                                     :description "вставить скрины из приложения и сказать что если добавлять баннеры - вот тут появится то что вы вставили. можно добавлять картинки либо гифки"}
                      "payment"     {:title "Способы оплаты"
                                     :description "тут можно подключить способы оплаты заказа, из представленных на выбор"}
                      "delivery"    {:title "Способы доставки"
                                     :description "тут можно подключить способы доставки заказа, из представленных на выбор"}
                      "managers"    {:title "Менеджеры"
                                     :description "тут можно регулировать кто кроме вас имеет доступ к админке. можете добавлять сотрудников, которые будут работать в админке"}
                      "store_name"  {:title "Название магазина"
                                     :description "скрин где отмечено где будет отображаться выбранное название"}
                      "contact"     {:title "Контактный аккаунт"
                                     :description "тоже скрин где будет отображаться"}
                      "description" {:title "Описание бота"
                                     :description "тоже скрин где будет отображаться"}
                      "welcome"     {:title "Приветственное сообщение"
                                     :description "тоже скрин где будет отображаться"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :settings_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
