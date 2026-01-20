package me.user.moc.command;

import me.user.moc.MocPlugin;
import me.user.moc.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * MocCommand: 플레이어가 채팅창에 치는 '/moc' 명령어를 인식하고 처리하는 클래스입니다.
 */
public class MocCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 명령어를 친 사람이 '플레이어'인지 확인합니다. (콘솔창에서 치는 건 무시)
        if (!(sender instanceof Player p)) return false;

        // 2. [오류 해결 부분] GameManager를 가져올 때 플러그인 정보를 함께 넘겨줍니다.
        // MocPlugin.getInstance()를 통해 현재 켜져 있는 플러그인을 찾고,
        // 그 플러그인 안에 있는 GameManager를 불러옵니다.
        GameManager gm = GameManager.getInstance(MocPlugin.getInstance());

        // 3. 만약 '/moc'만 치고 뒤에 아무것도 안 적었다면 명령어를 종료합니다.
        if (args.length == 0) return false;

        // 4. '/moc [명령어]' 에서 [명령어] 부분이 무엇인지에 따라 동작을 나눕니다.
        switch (args[0].toLowerCase()) {

            case "start" -> { // 게임 시작 (/moc start)
                if (!p.isOp()) { // 관리자(OP) 권한이 있는지 확인
                    p.sendMessage("§c권한이 없습니다.");
                    return true;
                }
                gm.startGame(p); // GameManager에게 게임 시작을 요청합니다.
                return true;
            }

            case "stop" -> { // 게임 종료 (/moc stop)
                if (!p.isOp()) {
                    p.sendMessage("§c권한이 없습니다.");
                    return true;
                }
                gm.stopGame(); // GameManager에게 게임 종료를 요청합니다.
                return true;
            }

            case "yes" -> { // 능력 수락 (/moc yes)
                gm.playerReady(p); // "나 이 능력으로 할게!"라고 등록합니다.
                return true;
            }

            case "re" -> { // 리롤 (/moc re)
                gm.playerReroll(p); // 능력을 다시 뽑습니다. (리롤 횟수 차감됨)
                return true;
            }

            case "check" -> { // 능력 상세 정보 확인 (/moc check)
                gm.showAbilityDetail(p); // 내가 가진 능력이 어떤 능력인지 자세히 보여줍니다.
                return true;
            }

            case "afk" -> { // 잠수 유저 설정 (/moc afk [닉네임])
                if (args.length < 2) {
                    p.sendMessage("§c사용법: /moc afk [플레이어이름]");
                    return true;
                }
                gm.toggleAfk(args[1]); // 해당 플레이어를 게임 참가 대상에서 제외하거나 다시 넣습니다.
                p.sendMessage("§e[MOC] §f" + args[1] + "님의 참가 상태를 변경했습니다.");
                return true;
            }
        }

        return false;
    }
}