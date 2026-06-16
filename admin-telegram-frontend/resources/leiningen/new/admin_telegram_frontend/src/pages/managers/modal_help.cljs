(ns {{name}}.pages.managers.modal-help
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:managers_help_modal_open?])
        help-type (reagent/cursor app-state [:managers_help_type])
        help-content {"managers_page" {:title "Менеджеры"
                                       :description "на этой странице можно давать и отнимать доступ к приложению. доступ к этой странице есть только у владельца магазина"}
                      "owners"        {:title "Владельцы"
                                       :description "Владельцы магазина имеют право добавлять и удалять менеджеров"}
                      "managers_list" {:title "Менеджеры"
                                       :description "менеджеры имеют доступ ко всему функционалу админки, кроме добавления других менеджеров"}
                      "requests"      {:title "Заявки на менеджера"
                                       :description "в списке отображены люди, которые подали заявку стать менеджером. чтобы подать заявку нужно отправить административному боту (@bot_username) команду /request"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :managers_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
