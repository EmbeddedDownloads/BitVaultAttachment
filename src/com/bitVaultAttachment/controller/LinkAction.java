package com.bitVaultAttachment.controller;

import com.bitVaultAttachment.models.NotificationListDTO;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;

/**
 * Created by vvdn on 5/26/2017.
 */
public class LinkAction extends TableCell<NotificationListDTO, Hyperlink> {

    private final Hyperlink link;

    public LinkAction() {
        link = new Hyperlink("Remove");
        link.setOnAction(evt -> {
            // remove row item from tableview
            getTableView().getItems().remove(getTableRow().getIndex());
        });
    }
}