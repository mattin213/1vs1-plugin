package mattin.vs1;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class GameManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, GamePlayer> players;
    private final ScoreboardManager scoreboardManager;
    private final Scoreboard scoreboard;
    private boolean running;


    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.players = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.scoreboardManager = Bukkit.getScoreboardManager();
        this.scoreboard = scoreboardManager.getNewScoreboard();
    }

    @EventHandler
    public void addPlayer(PlayerJoinEvent event) {
        if (hasPlayer(event.getPlayer())) {
            event.getPlayer().kickPlayer("Ein Fehler ist aufgetreten!");
            return;
        }
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.loadPlayerData();
        players.put(player.getUniqueId(), gamePlayer);
        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', "&8[&c1vs1&8]&r&l >>> " + event.getPlayer().getName()));
        if (Bukkit.getOnlinePlayers().size() == 1) {
            Location loc1 = new Location(Bukkit.getWorld("world"), -542, 8, -885);
            player.teleport(loc1);
            player.sendMessage(ChatColor.RED+"Du musst noch auf einen anderen Spieler warten!");
        }
        if (Bukkit.getOnlinePlayers().size() == 2) {
            Player[] playerArray = players.values().stream().map(GamePlayer::getPlayer).toArray(Player[]::new);
            Location loc1 = new Location(Bukkit.getWorld("world"), -542, 8, -903);
            player.teleport(loc1);
            startGame();
            Player player1 = playerArray[0];
            Location loc2 = new Location(Bukkit.getWorld("world"), -542, 8, -885);
            player1.teleport(loc2);
        }
        if (Bukkit.getOnlinePlayers().size() > 2){
           gamePlayer.isSpectator = true;
           gamePlayer.getPlayer().setGameMode(GameMode.SPECTATOR);
           Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',"[&c1vs1&8]&r&l "+player.getName()+"&r beobachtet das Spiel!"));
        }
        updateScoreboard(gamePlayer);
    }

    public void updateScoreboard(GamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("1vs1", "dummy", ChatColor.BOLD + "1vs1 - Classic");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score killsScore = objective.getScore(ChatColor.BLUE + "Kills / Deaths:");
        killsScore.setScore(4);
        String kills = String.valueOf(gamePlayer.getKills());
        String deaths = String.valueOf(gamePlayer.getDeaths());
        String kvS = kills + " / " + deaths;
        Score killsValue = objective.getScore(ChatColor.GREEN + kvS);
        killsValue.setScore(3);
        Score deathsScore = objective.getScore(ChatColor.BLUE + "KD:");
        deathsScore.setScore(2);
        String kd = String.valueOf(gamePlayer.getKills()/gamePlayer.getDeaths());
        Score deathsValue = objective.getScore(ChatColor.GREEN+kd);
        deathsValue.setScore(1);
        Score status = objective.getScore(ChatColor.RESET + "powered by " +ChatColor.GOLD+"LolinoTV");
        status.setScore(0);
        player.setScoreboard(scoreboard);
    }


    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public boolean hasPlayer(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public void startGame() {
        this.running = true;
        Player[] playerArray = players.values().stream().map(GamePlayer::getPlayer).toArray(Player[]::new);
        Player player1 = playerArray[0];
        Player player2 = playerArray[1];

        GamePlayer gamePlayer1 = getGamePlayer(player1);
        GamePlayer gamePlayer2 = getGamePlayer(player2);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(player1.getName() + " vs " + player2.getName());
            player.sendTitle(ChatColor.GREEN+"Viel Glück!", "");
            Bukkit.getWorld("world").setGameRule(GameRule.KEEP_INVENTORY, true);
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 10));
            player.getInventory().setItem(2, new ItemStack(Material.BOW));
            player.getInventory().setItem(8, new ItemStack(Material.ARROW, 32));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage("");
        if (!running) {
            running = false;
            GamePlayer gamePlayer = getGamePlayer(Bukkit.getPlayer(event.getEntity().getKiller().getName()));
            Player winner = gamePlayer.getPlayer();
            Bukkit.broadcastMessage(ChatColor.GOLD + gamePlayer.getPlayer().getName() + ChatColor.RESET + " hat das Spiel gewonnen!");
            //Bukkit.broadcastMessage("Das Replay dieser Runde lautet: "+ChatColor.BOLD+ChatColor.GOLD+ ReplayAPI.getReplayID());
            getGamePlayer(event.getEntity().getKiller()).setKills(getGamePlayer(event.getEntity().getKiller()).getKills() + 1);
            getGamePlayer(event.getEntity().getPlayer()).setDeaths(getGamePlayer(event.getEntity().getPlayer()).getDeaths() + 1);
            getGamePlayer(event.getEntity().getKiller()).savePlayerData();
            getGamePlayer(event.getEntity().getPlayer()).savePlayerData();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 100, 1);
            }
            Entity dragon = winner.getWorld().spawnEntity(winner.getLocation(), EntityType.ENDER_DRAGON);
            dragon.addPassenger(winner);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                dragon.remove();
                winner.setFallDistance(-100.0F);
                Bukkit.shutdown();
            }, 200L);
        }
        else {
            return;
        }
    }

    @EventHandler
    public void motdEvent(ServerListPingEvent event) {
        event.setMaxPlayers(3);
        if (running) {
            event.setMotd("[Spectate]");
        }
        else {
            event.setMotd("Warte...");
        }
    }
    @EventHandler
    public void dropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void destroyBlock(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.shutdown();
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!running) {
            event.setCancelled(true);
        }
    }

    public class GamePlayer {
        private final Player player;
        private int score;
        private int kills;
        private int deaths;

        public GamePlayer(Player player) {
            this.player = player;
            this.score = 0;
            this.kills = 0;
            this.deaths = 0;
        }
        public Boolean isSpectator = false;

        public Player getPlayer() {
            return player;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }

        public void loadPlayerData() {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/1vs1", "root", "160107")) {
                String query = "SELECT * FROM classic_player_data WHERE uuid=?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, player.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.score = rs.getInt("score");
                    this.kills = rs.getInt("kills");
                    this.deaths = rs.getInt("deaths");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        public void savePlayerData() {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/1vs1", "root", "160107")) {
                String query = "INSERT INTO classic_player_data (uuid, name, score, kills, deaths) VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE name=?, score=?, kills=?, deaths=?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setInt(3, score);
                ps.setInt(4, kills);
                ps.setInt(5, deaths);
                ps.setString(6, player.getName());
                ps.setInt(7, score);
                ps.setInt(8, kills);
                ps.setInt(9, deaths);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
