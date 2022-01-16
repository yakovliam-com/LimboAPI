/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.proxy.Player;
import java.util.concurrent.CompletableFuture;
import net.elytrium.limboapi.api.player.LimboPlayer;

public interface Limbo {

  void spawnPlayer(Player player, LimboSessionHandler handler);

  CompletableFuture<LimboPlayer> spawnLimboPlayer(Player apiPlayer, LimboSessionHandler handler);

  void respawnPlayer(Player player);
}
